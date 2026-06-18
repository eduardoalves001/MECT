package deti.sd.moss.core.manager;

import java.util.List;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.StringDataType;
import org.h2.mvstore.type.IntegerDataType;

import deti.sd.moss.core.common.TicketCodec;
import deti.sd.moss.core.common.ports.ServiceDiscovery;

import deti.sd.moss.core.manager.ports.ManagerServiceProvider;
import deti.sd.moss.core.manager.model.*;

import deti.sd.moss.core.volume.model.AssignVolumeRequest;
import deti.sd.moss.core.volume.model.AssignVolumeReply;

public class ManagerServiceProviderImpl implements ManagerServiceProvider {
    private static final Logger logger = LoggerFactory.getLogger(ManagerServiceProviderImpl.class);

    private static final int MAX_VOLUME_SIZE = 1 << 25; // 32 MiB

    // === Persistance ===
    private final MVStore store;
    private final MVMap<String, Integer> ledger;

    // === Counters ===
    private final AtomicInteger fidCounter;
    private final AtomicInteger vidCounter;

    // === Services ===
    private final ServiceDiscovery discovery;
    private final VolumeRegistry registry;
    private final ReentrantLock assignLock = new ReentrantLock();

    // ---
    public ManagerServiceProviderImpl(String mdir, ServiceDiscovery discovery) {
        this.discovery = discovery;
        this.registry = new VolumeRegistry();

        Path base = Path.of(mdir);
        Path data = base.resolve("manager.db");

        store = new MVStore.Builder()
                .fileName(data.toString())
                .open();

        ledger = store.openMap("ledger",
                new MVMap.Builder<String, Integer>()
                        .keyType(StringDataType.INSTANCE)
                        .valueType(IntegerDataType.INSTANCE));

        fidCounter = new AtomicInteger(ledger.getOrDefault("fid", 0));
        vidCounter = new AtomicInteger(ledger.getOrDefault("vid", 0));
    }

    @Override
    public AssignReply onAssign(AssignRequest request) {
        assignLock.lock();
        try {
            if (registry.getActiveNodeCount() < 2) {
                return new AssignReply("", "", 0, List.of());
            }

            var bestVolume = registry.selectBestVolume();
            int vid;
            String volumeUrl;

            if (bestVolume.isPresent()) {
                var volume = bestVolume.get();
                vid = volume.vid();
                volumeUrl = volume.nodeUrl();
            } else {
                var nodeUrl = registry.selectNodeForNewVolume();
                if (nodeUrl.isEmpty()) {
                    return new AssignReply("", "", 0, List.of());
                }

                int newVid = vidCounter.getAndIncrement();
                var assignReply = discovery.getVolume(nodeUrl.get())
                        .assignVolume(new AssignVolumeRequest(newVid, List.of()));
                if (assignReply.status() != 0) {
                    return new AssignReply("", "", 0, List.of());
                }
                ledger.put("vid", vidCounter.get());
                store.commit();

                vid = newVid;
                volumeUrl = nodeUrl.get();
            }

            int fid = fidCounter.getAndIncrement();
            int cookie = TicketCodec.generateCookie();
            String ticket = TicketCodec.create(vid, fid, cookie);
            ledger.put("fid", fidCounter.get());
            store.commit();

            return new AssignReply(ticket, volumeUrl, 0, List.of());
        } finally {
            assignLock.unlock();
        }
    }

    @Override
    public LookupReply onLookup(LookupRequest request) {
        try {
            var ticketInfo = TicketCodec.parse(request.ticket());
            var volumeUrl = registry.resolve(ticketInfo.vid());
            return new LookupReply(volumeUrl.orElse(""));
        } catch (RuntimeException ex) {
            logger.warn("Invalid lookup ticket: {}", request.ticket());
            return new LookupReply("");
        }
    }

    @Override
    public VolumeBeatReply onHeartbeat(VolumeBeatRequest request) {
        logger.info("Heartbeat received from {} with {} volumes", request.url(), request.count());
        registry.update(request.url(), request.vinfo(), Instant.now());
        return new VolumeBeatReply(0);
    }

    @Override
    public StateReply onState() {
        var snapshot = registry.snapshot();
        return new StateReply(snapshot.nodes(), snapshot.volumes());
    }
}
