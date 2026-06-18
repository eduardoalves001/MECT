package deti.sd.moss.core.manager;

import deti.sd.moss.core.common.model.VolumeInfo;
import deti.sd.moss.core.manager.model.NodeState;
import deti.sd.moss.core.manager.model.VolumeState;
import deti.sd.moss.core.volume.Volume;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class VolumeRegistry {

    static final int MAX_NUMBER_VOLUMES = 5;
    private static final int STALE_TIMEOUT_SECONDS = 20;
    private static final double CONSERVATIVE_CAPACITY_RATIO = 0.9;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<String, NodeRecord> nodes = new HashMap<>();
    private final Map<Integer, VolumeRecord> volumes = new HashMap<>();

    public void update(String nodeUrl, List<VolumeInfo> infos, Instant timestamp) {
        rwLock.writeLock().lock();
        try {
            Set<Integer> vids = infos.stream()
                    .map(VolumeInfo::vid)
                    .collect(Collectors.toSet());

            // Keep the registry in sync with the latest heartbeat payload.
            volumes.entrySet().removeIf(entry ->
                    nodeUrl.equals(entry.getValue().nodeUrl()) && !vids.contains(entry.getKey()));

            nodes.put(nodeUrl, new NodeRecord(nodeUrl, timestamp, vids));
            for (VolumeInfo info : infos) {
                volumes.put(info.vid(), new VolumeRecord(info.vid(), nodeUrl, info.fileCount(), info.availableSize(), info.status()));
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Optional<VolumeRecord> selectBestVolume() {
        rwLock.readLock().lock();
        try {
            Instant staleThreshold = Instant.now().minusSeconds(STALE_TIMEOUT_SECONDS);
            int minAvailable = (int) (Volume.MAX_VOLUME_SIZE * (1.0 - CONSERVATIVE_CAPACITY_RATIO));
            return volumes.values().stream()
                    .filter(v -> {
                        NodeRecord node = nodes.get(v.nodeUrl());
                        return node != null && node.lastSeen().isAfter(staleThreshold);
                    })
                    .filter(v -> v.availableSize() >= minAvailable)
                    .max(Comparator.comparingInt(VolumeRecord::availableSize));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Optional<String> selectNodeForNewVolume() {
        rwLock.readLock().lock();
        try {
            Instant staleThreshold = Instant.now().minusSeconds(STALE_TIMEOUT_SECONDS);
            return nodes.values().stream()
                    .filter(n -> n.lastSeen().isAfter(staleThreshold))
                    .filter(n -> n.vids().size() < MAX_NUMBER_VOLUMES)
                    .min(Comparator.comparingInt(n -> n.vids().size()))
                    .map(NodeRecord::url);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Optional<String> resolve(int vid) {
        rwLock.readLock().lock();
        try {
            VolumeRecord volume = volumes.get(vid);
            if (volume == null) return Optional.empty();
            NodeRecord node = nodes.get(volume.nodeUrl());
            if (node == null) return Optional.empty();
            Instant staleThreshold = Instant.now().minusSeconds(STALE_TIMEOUT_SECONDS);
            if (!node.lastSeen().isAfter(staleThreshold)) return Optional.empty();
            return Optional.of(volume.nodeUrl());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getActiveNodeCount() {
        rwLock.readLock().lock();
        try {
            Instant staleThreshold = Instant.now().minusSeconds(STALE_TIMEOUT_SECONDS);
            return (int) nodes.values().stream()
                    .filter(n -> n.lastSeen().isAfter(staleThreshold))
                    .count();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public ClusterState snapshot() {
        rwLock.readLock().lock();
        try {
            Instant staleThreshold = Instant.now().minusSeconds(STALE_TIMEOUT_SECONDS);

            List<NodeState> nodeStates = nodes.values().stream()
                    .map(node -> new NodeState(
                            node.url(),
                            node.lastSeen().isAfter(staleThreshold),
                            node.lastSeen().toString()))
                .collect(Collectors.toList());

            List<VolumeState> volumeStates = volumes.values().stream()
                    .map(volume -> {
                    NodeRecord node = nodes.get(volume.nodeUrl());
                    boolean online = node != null && node.lastSeen().isAfter(staleThreshold);
                    int status = online ? volume.status() : 0;
                    return new VolumeState(
                        volume.vid(),
                        volume.nodeUrl(),
                        volume.fileCount(),
                        volume.availableSize(),
                        status);
                    })
                .collect(Collectors.toList());

            return new ClusterState(List.copyOf(nodeStates), List.copyOf(volumeStates));
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
