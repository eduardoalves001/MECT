package deti.sd.moss.infra.rpc.discovery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import deti.sd.moss.core.common.ports.ServiceDiscovery;
import deti.sd.moss.core.manager.ports.ManagerService;
import deti.sd.moss.core.volume.ports.VolumeService;
import deti.sd.moss.core.object.ports.ObjectService;

import deti.sd.moss.infra.rpc.manager.ManagerGrpcAdapter;
import deti.sd.moss.infra.rpc.volume.VolumeGrpcAdapter;
import deti.sd.moss.infra.rpc.object.ObjectGrpcAdapter;


public class GrpcServiceDiscovery implements ServiceDiscovery {
    private final Map<String, ManagedChannel> cache = new ConcurrentHashMap<>();

    @Override
    public ManagerService getManager(String name) {
        var channel = cache.compute(name, (key, existingChannel) -> {
            if (existingChannel != null )
                return existingChannel;

            return ManagedChannelBuilder
                .forTarget(key)
                .usePlaintext()
                .build();
        });

        return new ManagerGrpcAdapter(channel);
    }

    @Override
    public ObjectService getObject(String name) {

        var channel = cache.compute(name, (key, existingChannel) -> {
            if (existingChannel != null )
                return existingChannel;

            return ManagedChannelBuilder
                .forTarget(key)
                .usePlaintext()
                .build();
        });

        return new ObjectGrpcAdapter(channel);
    }

    @Override
    public VolumeService getVolume(String name) {

        var channel = cache.compute(name, (key, existingChannel) -> {
            if (existingChannel != null )
                return existingChannel;

            return ManagedChannelBuilder
                .forTarget(key)
                .usePlaintext()
                .build();
        });

        return new VolumeGrpcAdapter(channel);
    }
}
