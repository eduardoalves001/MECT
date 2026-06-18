// Tests generated with AI assistance
package deti.sd.moss.core.object;

import deti.sd.moss.core.common.TicketCodec;
import deti.sd.moss.core.common.ports.ServiceDiscovery;
import deti.sd.moss.core.manager.model.AssignReply;
import deti.sd.moss.core.manager.model.AssignRequest;
import deti.sd.moss.core.manager.model.LookupReply;
import deti.sd.moss.core.manager.model.LookupRequest;
import deti.sd.moss.core.manager.model.VolumeBeatReply;
import deti.sd.moss.core.manager.model.VolumeBeatRequest;
import deti.sd.moss.core.manager.model.StateReply;
import deti.sd.moss.core.manager.ports.ManagerService;
import deti.sd.moss.core.object.model.GetReply;
import deti.sd.moss.core.object.model.GetRequest;
import deti.sd.moss.core.object.model.ListReply;
import deti.sd.moss.core.object.model.ListRequest;
import deti.sd.moss.core.object.model.PutRequest;
import deti.sd.moss.core.volume.model.AssignVolumeReply;
import deti.sd.moss.core.volume.model.AssignVolumeRequest;
import deti.sd.moss.core.volume.model.CompactReply;
import deti.sd.moss.core.volume.model.CompactRequest;
import deti.sd.moss.core.volume.model.DeleteReply;
import deti.sd.moss.core.volume.model.DeleteRequest;
import deti.sd.moss.core.volume.model.ReadReply;
import deti.sd.moss.core.volume.model.ReadRequest;
import deti.sd.moss.core.volume.model.WriteReply;
import deti.sd.moss.core.volume.model.WriteRequest;
import deti.sd.moss.core.volume.ports.VolumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectServiceProviderImplTest {

    private static final String VOLUME_URL = "volume:1234";
    private static final String TICKET = TicketCodec.create(1, 2, 3);
    private static final byte[] DATA = "hello".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path tempDir;

    private ObjectServiceProviderImpl provider;
    private AtomicInteger lookupCount;

    @BeforeEach
    void setUp() {
        lookupCount = new AtomicInteger();
        ManagerService manager = new ManagerService() {
            @Override
            public AssignReply assign(AssignRequest request) {
                return new AssignReply(TICKET, VOLUME_URL, 0, List.of());
            }

            @Override
            public LookupReply lookup(LookupRequest request) {
                lookupCount.incrementAndGet();
                return new LookupReply(VOLUME_URL);
            }

            @Override
            public VolumeBeatReply heartbeat(VolumeBeatRequest request) {
                return new VolumeBeatReply(0);
            }

            @Override
            public StateReply state() {
                return new StateReply(List.of(), List.of());
            }
        };

        VolumeService volume = new VolumeService() {
            @Override
            public AssignVolumeReply assignVolume(AssignVolumeRequest request) {
                return new AssignVolumeReply(0);
            }

            @Override
            public WriteReply write(WriteRequest request) {
                return new WriteReply(0);
            }

            @Override
            public ReadReply read(ReadRequest request) {
                if (request.vid() == 1 && request.fid() == 2 && request.cookie() == 3) {
                    return new ReadReply(0, DATA);
                }
                return new ReadReply(1, new byte[0]);
            }

            @Override
            public DeleteReply delete(DeleteRequest request) {
                return new DeleteReply(0);
            }

            @Override
            public CompactReply compact(CompactRequest request) {
                return new CompactReply(0);
            }
        };

        ServiceDiscovery discovery = new ServiceDiscovery() {
            @Override
            public ManagerService getManager(String name) {
                return manager;
            }

            @Override
            public deti.sd.moss.core.object.ports.ObjectService getObject(String name) {
                return null;
            }

            @Override
            public VolumeService getVolume(String name) {
                return volume;
            }
        };

        String dbPath = tempDir.resolve("obj.db").toString();
        provider = new ObjectServiceProviderImpl("manager:1234", dbPath, discovery);
    }

    @Test
    void onGetReturnsStoredData() {
        provider.onPut(new PutRequest("bucket", "path", DATA));

        GetReply reply = provider.onGet(new GetRequest("bucket", "path"));
        assertEquals(0, reply.status());
        assertArrayEquals(DATA, reply.data());
    }

    @Test
    void onGetReturnsErrorWhenMissing() {
        GetReply reply = provider.onGet(new GetRequest("bucket", "missing"));
        assertEquals(1, reply.status());
        assertEquals(0, reply.data().length);
    }

    @Test
    void onGetUsesCachedVolumeUrl() {
        provider.onPut(new PutRequest("bucket", "path", DATA));

        provider.onGet(new GetRequest("bucket", "path"));
        provider.onGet(new GetRequest("bucket", "path"));

        assertEquals(1, lookupCount.get());
    }
}
