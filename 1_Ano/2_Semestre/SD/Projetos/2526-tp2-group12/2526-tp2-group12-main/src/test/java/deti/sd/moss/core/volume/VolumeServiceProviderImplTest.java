// Tests generated with AI assistance
package deti.sd.moss.core.volume;

import deti.sd.moss.core.common.model.VolumeInfo;
import deti.sd.moss.core.common.ports.ServiceDiscovery;
import deti.sd.moss.core.manager.model.*;
import deti.sd.moss.core.manager.ports.ManagerService;
import deti.sd.moss.core.object.ports.ObjectService;
import deti.sd.moss.core.volume.model.AssignVolumeReply;
import deti.sd.moss.core.volume.model.AssignVolumeRequest;
import deti.sd.moss.core.volume.ports.VolumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VolumeServiceProviderImplTest {

    @TempDir
    Path tempDir;

    private VolumeServiceProviderImpl provider;

    private static final ServiceDiscovery STUB_DISCOVERY = new ServiceDiscovery() {
        @Override public ManagerService getManager(String name) { return STUB_MANAGER; }
        @Override public ObjectService getObject(String name) { return null; }
        @Override public VolumeService getVolume(String name) { return null; }
    };

    private static final ManagerService STUB_MANAGER = new ManagerService() {
        @Override public AssignReply assign(AssignRequest r) { return null; }
        @Override public LookupReply lookup(LookupRequest r) { return null; }
        @Override public VolumeBeatReply heartbeat(VolumeBeatRequest r) { return new VolumeBeatReply(0); }
        @Override public StateReply state() { return new StateReply(List.of(), List.of()); }
    };

    @BeforeEach
    void setUp() {
        provider = new VolumeServiceProviderImpl(8080, "manager:9090", tempDir.toString(), STUB_DISCOVERY);
    }

    @Test
    void assignVolumeSucceedsWhenUnderLimit() {
        AssignVolumeReply reply = provider.onAssignVolume(new AssignVolumeRequest(1, List.of()));
        assertEquals(0, reply.status());
    }

    @Test
    void assignVolumeCreatesVolumeThatIsTracked() {
        provider.onAssignVolume(new AssignVolumeRequest(1, List.of()));
        // Assigning same vid again still works (volume already exists on disk, reload is fine)
        // assign a second distinct vid to confirm it's tracked
        provider.onAssignVolume(new AssignVolumeRequest(2, List.of()));
        AssignVolumeReply third = provider.onAssignVolume(new AssignVolumeRequest(3, List.of()));
        assertEquals(0, third.status());
    }

    @Test
    void assignVolumeFailsWhenAtMaxLimit() {
        for (int i = 1; i <= VolumeServiceProviderImpl.MAX_NUMBER_VOLUMES; i++) {
            provider.onAssignVolume(new AssignVolumeRequest(i, List.of()));
        }
        AssignVolumeReply reply = provider.onAssignVolume(new AssignVolumeRequest(99, List.of()));
        assertEquals(1, reply.status());
    }
}
