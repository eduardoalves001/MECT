package deti.sd.moss.core.object;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.StringDataType;
import org.h2.mvstore.MVMap.Builder;

import deti.sd.moss.core.common.TicketCodec;
import deti.sd.moss.core.common.ports.ServiceDiscovery;
import deti.sd.moss.core.manager.ports.ManagerService;
import deti.sd.moss.core.volume.ports.VolumeService;
import deti.sd.moss.core.object.ports.ObjectServiceProvider;

import deti.sd.moss.core.manager.model.*;
import deti.sd.moss.core.volume.model.*;
import deti.sd.moss.core.object.model.*;

public class ObjectServiceProviderImpl implements ObjectServiceProvider {
    private static final Logger logger = LoggerFactory.getLogger(ObjectServiceProviderImpl.class);

    // === Communications ===
    private final ServiceDiscovery discovery;
    private final ManagerService manager;

    // === Persistence ===
    private final MVStore store;
    private final MVMap<String, ObjectEntry> objects;
    private final ConcurrentHashMap<Integer, String> volumeUrlCache = new ConcurrentHashMap<>();

    public ObjectServiceProviderImpl(String managerUrl, String dbname, ServiceDiscovery discovery) {
        this.discovery = discovery;
        this.manager = discovery.getManager(managerUrl);

        // Initialize MVStore for objects metadata
        this.store = new MVStore.Builder().fileName(dbname).open();
        this.objects = store.openMap("objects",
                new Builder<String, ObjectEntry>().keyType(StringDataType.INSTANCE)
                        .valueType(new ObjectEntryDataType()));

        // Ensure the store is closed on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                store.close();
            } catch (Exception e) {
                logger.warn("Failed to close MVStore: {}", e.getMessage());
            }
        }));
    }

    @Override
    public GetReply onGet(GetRequest request) {
        String key = request.bucket() + "/" + request.path();
        ObjectEntry entry = objects.get(key);

        if (entry == null || entry.ticket().isEmpty()) {
            logger.warn("Object not found for key {}", key);
            return new GetReply(1, new byte[0]);
        }

        String ticket = entry.ticket().get(0);
        var ticketInfo = TicketCodec.parse(ticket);
        int vid = ticketInfo.vid();
        int fid = ticketInfo.fid();
        int cookie = ticketInfo.cookie();

        String volumeUrl = volumeUrlCache.get(vid);
        if (volumeUrl == null || volumeUrl.isEmpty()) {
            LookupReply lookupReply = manager.lookup(new LookupRequest(ticket));
            volumeUrl = lookupReply.url();
            if (volumeUrl == null || volumeUrl.isEmpty()) {
                logger.warn("Failed to resolve volume for ticket {}", ticket);
                return new GetReply(1, new byte[0]);
            }
            volumeUrlCache.put(vid, volumeUrl);
        }

        VolumeService volumeService = discovery.getVolume(volumeUrl);
        ReadReply readReply = volumeService.read(new ReadRequest(vid, fid, cookie));

        if (readReply.status() != 0) {
            logger.warn("Failed to read object from volume {}", vid);
            return new GetReply(1, new byte[0]);
        }

        return new GetReply(0, readReply.data());
    }

    @Override
    public PutReply onPut(PutRequest request) {
        // 1. Request volume assignment from manager
        AssignReply assignReply = manager.assign(new AssignRequest(1));
        String ticket = assignReply.ticket();
        String volumeUrl = assignReply.volumeUrl();

        if (ticket == null || ticket.isEmpty() || volumeUrl == null || volumeUrl.isEmpty()) {
            logger.warn("Failed to assign volume for Put operation");
            return new PutReply(1);
        }

        // 2. Parse ticket to extract vid, fid, cookie
        var ticketInfo = TicketCodec.parse(ticket);
        int vid = ticketInfo.vid();
        int fid = ticketInfo.fid();
        int cookie = ticketInfo.cookie();

        // 3. Write data to the volume
        VolumeService volumeService = discovery.getVolume(volumeUrl);
        WriteReply writeReply = volumeService.write(new WriteRequest(vid, fid, cookie, request.data()));

        if (writeReply.status() != 0) {
            logger.warn("Failed to write data to volume {}", vid);
            return new PutReply(1);
        }

        // 4. Create and store object entry in metadata database
        ObjectEntry entry = new ObjectEntry(
                List.of(ticket),
                Instant.now(),
                request.data().length);

        String key = request.bucket() + "/" + request.path();
        objects.put(key, entry);

        // 5. Commit the metadata changes
        store.commit();

        // 6. Return success
        return new PutReply(0);
    }

    @Override
    public RemoveReply onRemove(RemoveRequest request) {
        String key = request.bucket() + "/" + request.path();
        ObjectEntry entry = objects.get(key);

        if (entry == null || entry.ticket().isEmpty()) {
            logger.warn("Object not found for removal: {}", key);
            return new RemoveReply(1);
        }

        String ticket = entry.ticket().get(0);
        var ticketInfo = TicketCodec.parse(ticket);
        int vid = ticketInfo.vid();
        int fid = ticketInfo.fid();
        int cookie = ticketInfo.cookie();

        String volumeUrl = volumeUrlCache.getOrDefault(vid, null);
        if (volumeUrl == null || volumeUrl.isEmpty()) {
            LookupReply lookupReply = manager.lookup(new LookupRequest(ticket));
            volumeUrl = lookupReply.url();
            if (volumeUrl == null || volumeUrl.isEmpty()) {
                logger.warn("Failed to resolve volume for removal of ticket {}", ticket);
                return new RemoveReply(1);
            }
            volumeUrlCache.put(vid, volumeUrl);
        }

        var deleteReply = discovery.getVolume(volumeUrl)
                .delete(new deti.sd.moss.core.volume.model.DeleteRequest(vid, fid, cookie));

        if (deleteReply.status() != 0) {
            logger.warn("Volume delete failed for fid={} vid={}", fid, vid);
            return new RemoveReply(1);
        }

        objects.remove(key);
        store.commit();
        return new RemoveReply(0);
    }

    @Override
    public ListReply onList(ListRequest request){
        String prefix = request.bucket() + "/";
        List<ListReply.ObjectInfo> result = new ArrayList<>();

        for (var entry : objects.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(prefix)) {
                continue;
            }

            ObjectEntry object = entry.getValue();
            result.add(new ListReply.ObjectInfo(
                key.substring(prefix.length()),
                object.size(),
                object.timestamp().toEpochMilli()
            ));
        }

        return new ListReply(0, result);
    }
}
