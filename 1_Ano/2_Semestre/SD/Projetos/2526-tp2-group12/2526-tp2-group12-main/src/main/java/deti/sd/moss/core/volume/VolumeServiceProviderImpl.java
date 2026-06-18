package deti.sd.moss.core.volume;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deti.sd.moss.core.common.ports.ServiceDiscovery;
import deti.sd.moss.core.manager.ports.ManagerService;
import deti.sd.moss.core.volume.ports.VolumeServiceProvider;

import deti.sd.moss.core.volume.model.*;
import java.io.IOException;

import deti.sd.moss.core.common.model.VolumeInfo;
import deti.sd.moss.core.manager.model.VolumeBeatRequest;
import deti.sd.moss.core.manager.model.VolumeBeatReply;

import deti.sd.moss.util.NetworkUtils;

public class VolumeServiceProviderImpl implements VolumeServiceProvider {
    private static final Logger logger = LoggerFactory.getLogger(VolumeServiceProviderImpl.class);

    static final int MAX_NUMBER_VOLUMES = 5;

    private final ConcurrentHashMap<Integer, Volume> volumes = new ConcurrentHashMap<>();
    private final ServiceDiscovery discovery;
    private final ManagerService manager;

    private final String basedir;
    private final String selfUrl;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    public VolumeServiceProviderImpl(int port, String managerUrl, String dir, ServiceDiscovery discovery) {
        this.discovery = discovery;
        this.manager = discovery.getManager(managerUrl);
        this.basedir = dir;

        this.selfUrl = String.format("%s:%d", NetworkUtils.getRealIPv4(), port);

        loadExistingVolumes();
        heartbeatExecutor.scheduleAtFixedRate(() -> sendHeartbeat(), 5, 5, TimeUnit.SECONDS);
    }

    private void loadExistingVolumes() {
        Path base = Path.of(basedir);
        if (!Files.isDirectory(base))
            return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base, "*.idx")) {
            for (Path idxFile : stream) {
                String filename = idxFile.getFileName().toString();
                int vid = Integer.parseInt(filename.replace(".idx", ""));
                Volume volume = new Volume(vid, basedir);
                volumes.put(vid, volume);
            }
        } catch (IOException e) {
            logger.error("Failed to load existing volumes", e);
        }

        logger.info("Loaded {} existing volumes from {}", volumes.size(), basedir);
    }

    @Override
    public AssignVolumeReply onAssignVolume(AssignVolumeRequest request) {
        if (volumes.size() >= MAX_NUMBER_VOLUMES) {
            return new AssignVolumeReply(1);
        }
        int vid = request.vid();
        Volume volume;
        try {
            volume = new Volume(vid, basedir);
        } catch (IOException e) {
            logger.error("Failed to create volume {}", vid, e);
            return new AssignVolumeReply(1);
        }
        volumes.put(vid, volume);
        return new AssignVolumeReply(0);
    }

    @Override
    public WriteReply onWrite(WriteRequest request) {
        Volume volume = volumes.get(request.vid());
        if (volume == null) {
            return new WriteReply(1);
        }
        try {
            WriteResult result = volume.write(request.fid(), request.cookie(), request.data());
            if (result == WriteResult.FULL) {
                sendHeartbeat();
                return new WriteReply(1);
            }
            if (result == WriteResult.DUPLICATE) {
                return new WriteReply(1);
            }
            return new WriteReply(0);
        } catch (IOException e) {
            logger.error("Failed to write fid {} to volume {}", request.fid(), request.vid(), e);
            return new WriteReply(1);
        }
    }

    public void sendHeartbeat() {
        List<VolumeInfo> infos = new ArrayList<>();
        for (Volume v : volumes.values()) {
            infos.add(v.getInfo());
        }
        try {
            manager.heartbeat(new VolumeBeatRequest(selfUrl, infos.size(), infos));
        } catch (Exception e) {
            logger.warn("Heartbeat failed, will retry on next interval", e);
        }
    }

    @Override
    public ReadReply onRead(ReadRequest request) {
        Volume volume = volumes.get(request.vid());
        if (volume == null) {
            return new ReadReply(1, new byte[0]);
        }
        try {
            byte[] data = volume.read(request.fid(), request.cookie());
            if (data == null) {
                return new ReadReply(1, new byte[0]);
            }
            return new ReadReply(0, data);
        } catch (IOException e) {
            logger.error("Failed to read fid {} from volume {}", request.fid(), request.vid(), e);
            return new ReadReply(1, new byte[0]);
        }
    }

    @Override
    public DeleteReply onDelete(DeleteRequest request) {
        Volume volume = volumes.get(request.vid());
        if (volume == null) {
            return new DeleteReply(1);
        }
        try {
            boolean deleted = volume.delete(request.fid(), request.cookie());
            return new DeleteReply(deleted ? 0 : 1);
        } catch (IOException e) {
            logger.error("Failed to delete fid {} from volume {}", request.fid(), request.vid(), e);
            return new DeleteReply(1);
        }
    }

    @Override
    public CompactReply onCompact(CompactRequest request) {
        Volume volume = volumes.get(request.vid());
        if (volume == null) {
            return new CompactReply(1);
        }
        try {
            volume.compact();
            sendHeartbeat();
            return new CompactReply(0);
        } catch (IOException e) {
            logger.error("Failed to compact volume {}", request.vid(), e);
            return new CompactReply(1);
        }
    }

    @Override
    public void shutdown() {
        heartbeatExecutor.shutdown();
    }
}
