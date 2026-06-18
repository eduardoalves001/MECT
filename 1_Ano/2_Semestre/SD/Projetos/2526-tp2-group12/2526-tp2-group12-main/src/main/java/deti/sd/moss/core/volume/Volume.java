package deti.sd.moss.core.volume;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deti.sd.moss.core.common.model.VolumeInfo;

public class Volume {
    private static final Logger logger = LoggerFactory.getLogger(Volume.class);

    public static final int MAX_VOLUME_SIZE = 1 << 25; // 32 MiB
    private static final int IDX_ENTRY_SIZE = 20; // fid(4) + cookie(4) + size(4) + offset(8)
    private static final int TOMBSTONE_SIZE = -1;

    private final int vid;
    private final Path idxPath;
    private final Path dataPath;
    private final ReentrantLock lock = new ReentrantLock();
    private final ConcurrentHashMap<Integer, IndexEntry> index = new ConcurrentHashMap<>();
    private final AtomicInteger usedBytes = new AtomicInteger(0);
    private final AtomicBoolean compacting = new AtomicBoolean(false);

    public Volume(int vid, String basedir) throws IOException {
        this.vid = vid;
        Path base = Path.of(basedir);
        Files.createDirectories(base);

        this.idxPath = base.resolve(vid + ".idx");
        this.dataPath = base.resolve(vid + ".data");

        if (!Files.exists(idxPath)) {
            Files.createFile(idxPath);
        }
        if (!Files.exists(dataPath)) {
            Files.createFile(dataPath);
        }

        reload();
    }

    public void reload() throws IOException {
        index.clear();
        usedBytes.set(0);

        byte[] raw = Files.readAllBytes(idxPath);
        ByteBuffer buf = ByteBuffer.wrap(raw);

        while (buf.remaining() >= IDX_ENTRY_SIZE) {
            int fid = buf.getInt();
            int cookie = buf.getInt();
            int size = buf.getInt();
            long offset = buf.getLong();

            if (size == TOMBSTONE_SIZE) {
                index.remove(fid);
            } else {
                IndexEntry existing = index.put(fid, new IndexEntry(fid, cookie, size, offset));
                if (existing == null) {
                    usedBytes.addAndGet(size);
                }
            }
        }

        logger.info("Volume {} loaded: {} files, {} bytes used", vid, index.size(), usedBytes.get());
    }

    public VolumeInfo getInfo() {
        int status = compacting.get() ? 0 : 1;
        return new VolumeInfo(vid, index.size(), MAX_VOLUME_SIZE - usedBytes.get(), status);
    }

    public int getVid() {
        return vid;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public ConcurrentHashMap<Integer, IndexEntry> getIndex() {
        return index;
    }

    public AtomicInteger getUsedBytes() {
        return usedBytes;
    }

    public Path getIdxPath() {
        return idxPath;
    }

    public Path getDataPath() {
        return dataPath;
    }

    public WriteResult write(int fid, int cookie, byte[] data) throws IOException {
        if (compacting.get()) {
            return WriteResult.FULL;
        }
        if (index.containsKey(fid)) {
            return WriteResult.DUPLICATE;
        }
        if (usedBytes.get() + data.length > MAX_VOLUME_SIZE) {
            return WriteResult.FULL;
        }
        lock.lock();
        try {
            if (index.containsKey(fid)) {
                return WriteResult.DUPLICATE;
            }
            long offset;
            try (RandomAccessFile raf = new RandomAccessFile(dataPath.toFile(), "rw")) {
                raf.seek(raf.length());
                offset = raf.getFilePointer();
                raf.write(data);
            }
            ByteBuffer entry = ByteBuffer.allocate(IDX_ENTRY_SIZE);
            entry.putInt(fid);
            entry.putInt(cookie);
            entry.putInt(data.length);
            entry.putLong(offset);
            Files.write(idxPath, entry.array(), java.nio.file.StandardOpenOption.APPEND);
            index.put(fid, new IndexEntry(fid, cookie, data.length, offset));
            usedBytes.addAndGet(data.length);
            return WriteResult.OK;
        } finally {
            lock.unlock();
        }
    }

    public byte[] read(int fid, int cookie) throws IOException {
        IndexEntry entry = index.get(fid);
        if (entry == null) {
            return null;
        }
        if (entry.cookie() != cookie) {
            return null;
        }
        byte[] data = new byte[entry.size()];
        try (RandomAccessFile raf = new RandomAccessFile(dataPath.toFile(), "r")) {
            raf.seek(entry.offset());
            raf.readFully(data);
        }
        return data;
    }

    public boolean delete(int fid, int cookie) throws IOException {
        IndexEntry entry = index.get(fid);
        if (entry == null) {
            return false;
        }
        if (entry.cookie() != cookie) {
            return false;
        }
        ByteBuffer tombstone = ByteBuffer.allocate(IDX_ENTRY_SIZE);
        tombstone.putInt(fid);
        tombstone.putInt(cookie);
        tombstone.putInt(TOMBSTONE_SIZE);
        tombstone.putLong(-1L);
        Files.write(idxPath, tombstone.array(), StandardOpenOption.APPEND);
        index.remove(fid);
        usedBytes.addAndGet(-entry.size());
        logger.info("Volume {} fid={} marked as tombstone", vid, fid);
        return true;
    }

    public void compact() throws IOException {
        compacting.set(true);
        lock.lock();
        try {
            Path tmpData = dataPath.resolveSibling(vid + ".data.tmp");
            Path tmpIdx = idxPath.resolveSibling(vid + ".idx.tmp");

            var liveEntries = new ArrayList<>(index.values());
            usedBytes.set(0);

            try (RandomAccessFile dataOut = new RandomAccessFile(tmpData.toFile(), "rw")) {
                try (var idxOut = Files.newOutputStream(tmpIdx, StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    for (IndexEntry e : liveEntries) {
                        byte[] data = new byte[e.size()];
                        try (RandomAccessFile dataIn = new RandomAccessFile(dataPath.toFile(), "r")) {
                            dataIn.seek(e.offset());
                            dataIn.readFully(data);
                        }
                        long newOffset = dataOut.length();
                        dataOut.seek(newOffset);
                        dataOut.write(data);

                        ByteBuffer entry = ByteBuffer.allocate(IDX_ENTRY_SIZE);
                        entry.putInt(e.fid());
                        entry.putInt(e.cookie());
                        entry.putInt(e.size());
                        entry.putLong(newOffset);
                        idxOut.write(entry.array());

                        index.put(e.fid(), new IndexEntry(e.fid(), e.cookie(), e.size(), newOffset));
                        usedBytes.addAndGet(e.size());
                    }
                }
            }

            Files.move(tmpData, dataPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Files.move(tmpIdx, idxPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            logger.info("Volume {} compacted: {} files, {} bytes", vid, index.size(), usedBytes.get());
        } finally {
            compacting.set(false);
            lock.unlock();
        }
    }
}
