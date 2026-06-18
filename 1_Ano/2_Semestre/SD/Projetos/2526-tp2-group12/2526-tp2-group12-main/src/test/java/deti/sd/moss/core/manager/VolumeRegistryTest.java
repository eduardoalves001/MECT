// Tests generated with AI assistance
package deti.sd.moss.core.manager;

import deti.sd.moss.core.common.model.VolumeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VolumeRegistryTest {

    private VolumeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new VolumeRegistry();
    }

    @Test
    void updateAddsNodeAndVolumes() {
        registry.update("node1", List.of(new VolumeInfo(1, 0, 33_554_432, 1)), Instant.now());

        assertEquals(1, registry.getActiveNodeCount());
    }

    @Test
    void getActiveNodeCountExcludesStaleNode() {
        Instant stale = Instant.now().minusSeconds(25);
        registry.update("old-node", List.of(new VolumeInfo(1, 0, 33_554_432, 1)), stale);

        assertEquals(0, registry.getActiveNodeCount());
    }

    @Test
    void selectBestVolumeReturnsMostAvailable() {
        registry.update("node1", List.of(
                new VolumeInfo(1, 10, 20_000_000, 1),
                new VolumeInfo(2, 5, 30_000_000, 1)
        ), Instant.now());

        Optional<VolumeRecord> best = registry.selectBestVolume();
        assertTrue(best.isPresent());
        assertEquals(2, best.get().vid());
    }

    @Test
    void selectBestVolumeExcludesVolumesAboveConservativeThreshold() {
        // availableSize < 10% of MAX_VOLUME_SIZE (33_554_432 * 0.1 = 3_355_443)
        registry.update("node1", List.of(new VolumeInfo(1, 100, 1_000_000, 1)), Instant.now());

        Optional<VolumeRecord> best = registry.selectBestVolume();
        assertTrue(best.isEmpty());
    }

    @Test
    void selectBestVolumeExcludesStaleNodes() {
        Instant stale = Instant.now().minusSeconds(25);
        registry.update("old-node", List.of(new VolumeInfo(1, 0, 30_000_000, 1)), stale);

        assertTrue(registry.selectBestVolume().isEmpty());
    }

    @Test
    void selectNodeForNewVolumeReturnsNodeWithFewestVolumes() {
        registry.update("node1", List.of(
                new VolumeInfo(1, 0, 30_000_000, 1),
                new VolumeInfo(2, 0, 30_000_000, 1)
        ), Instant.now());
        registry.update("node2", List.of(
                new VolumeInfo(3, 0, 30_000_000, 1)
        ), Instant.now());

        Optional<String> selected = registry.selectNodeForNewVolume();
        assertTrue(selected.isPresent());
        assertEquals("node2", selected.get());
    }

    @Test
    void selectNodeForNewVolumeReturnsEmptyWhenAllAtMax() {
        List<VolumeInfo> fullNode = List.of(
                new VolumeInfo(1, 0, 30_000_000, 1),
                new VolumeInfo(2, 0, 30_000_000, 1),
                new VolumeInfo(3, 0, 30_000_000, 1),
                new VolumeInfo(4, 0, 30_000_000, 1),
                new VolumeInfo(5, 0, 30_000_000, 1)
        );
        registry.update("node1", fullNode, Instant.now());

        assertTrue(registry.selectNodeForNewVolume().isEmpty());
    }

    @Test
    void resolveReturnsNodeUrlForAliveNode() {
        registry.update("node1", List.of(new VolumeInfo(1, 0, 30_000_000, 1)), Instant.now());

        Optional<String> url = registry.resolve(1);
        assertTrue(url.isPresent());
        assertEquals("node1", url.get());
    }

    @Test
    void resolveReturnsEmptyForStaleNode() {
        Instant stale = Instant.now().minusSeconds(25);
        registry.update("old-node", List.of(new VolumeInfo(1, 0, 30_000_000, 1)), stale);

        assertTrue(registry.resolve(1).isEmpty());
    }

    @Test
    void resolveReturnsEmptyForUnknownVid() {
        assertTrue(registry.resolve(99).isEmpty());
    }

    @Test
    void snapshotReturnsCopyOfNodes() {
        registry.update("node1", List.of(new VolumeInfo(1, 0, 30_000_000, 1)), Instant.now());

        ClusterState snap = registry.snapshot();
        assertEquals(1, snap.nodes().size());
        assertEquals("node1", snap.nodes().get(0).url());
        assertTrue(snap.nodes().get(0).online());
        assertEquals(1, snap.volumes().size());
        assertEquals(1, snap.volumes().get(0).vid());
        assertEquals("node1", snap.volumes().get(0).nodeUrl());
    }

    @Test
    void updateRemovesVolumesMissingFromLatestHeartbeat() {
        registry.update("node1", List.of(
                new VolumeInfo(1, 0, 30_000_000, 1),
                new VolumeInfo(2, 0, 29_000_000, 1)
        ), Instant.now());

        registry.update("node1", List.of(
                new VolumeInfo(2, 1, 28_000_000, 1)
        ), Instant.now());

        ClusterState snap = registry.snapshot();
        assertEquals(1, snap.volumes().size());
        assertEquals(2, snap.volumes().get(0).vid());
    }

    @Test
    void snapshotMarksStaleNodesOffline() {
        Instant stale = Instant.now().minusSeconds(25);
        registry.update("old-node", List.of(new VolumeInfo(1, 0, 30_000_000, 1)), stale);

        ClusterState snap = registry.snapshot();
        assertEquals(1, snap.nodes().size());
        assertFalse(snap.nodes().get(0).online());
        assertEquals(stale.toString(), snap.nodes().get(0).lastSeen());
        assertEquals(0, snap.volumes().get(0).status());
    }
}
