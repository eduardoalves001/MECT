package deti.sd.moss.infra.rpc.manager;

import io.grpc.stub.StreamObserver;

import deti.sd.moss.infra.rpc.manager.ManagerGrpc;
import deti.sd.moss.infra.rpc.manager.ProtoAssignRequest;
import deti.sd.moss.infra.rpc.manager.ProtoAssignReply;
import deti.sd.moss.infra.rpc.manager.ProtoLookupRequest;
import deti.sd.moss.infra.rpc.manager.ProtoLookupReply;
import deti.sd.moss.infra.rpc.manager.ProtoVolumeBeatRequest;
import deti.sd.moss.infra.rpc.manager.ProtoVolumeBeatReply;
import deti.sd.moss.infra.rpc.manager.ProtoStateRequest;
import deti.sd.moss.infra.rpc.manager.ProtoStateReply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deti.sd.moss.core.manager.ports.ManagerServiceProvider;
import deti.sd.moss.core.manager.model.*;

import deti.sd.moss.core.common.model.VolumeInfo;

public final class ManagerGrpcService extends ManagerGrpc.ManagerImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ManagerGrpcService.class);

    private final ManagerServiceProvider manager;

    public ManagerGrpcService(ManagerServiceProvider manager){
        this.manager = manager;
    }

    @Override
    public void assign(ProtoAssignRequest protoRequest, StreamObserver<ProtoAssignReply> responseObserver) {
        // 1. Map Protobuf -> Domain Model
        var request = new AssignRequest(protoRequest.getReplicas());

        // 2. Call the shared logic
        var response = manager.onAssign(request);
        logger.info("assign replicas={} → ticket={} url={}", request.replicas(), response.ticket(), response.volumeUrl());

        // 3. Map Domain Model -> Protobuf
        var reply = ProtoAssignReply.newBuilder()
            .setTicket(response.ticket())
            .setVolumeUrl(response.volumeUrl())
            .setCount(response.count())
            .addAllUrl(response.url())
            .build();

        // 4. Send gRPC response
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void lookup(ProtoLookupRequest protoRequest, StreamObserver<ProtoLookupReply> responseObserver) {
        // 1. Map Protobuf -> Domain Model
        var request = new LookupRequest(protoRequest.getTicket());

        // 2. Call the shared logic
        var response = manager.onLookup(request);
        logger.info("lookup ticket={} → url={}", request.ticket(), response.url());

        // 3. Map Domain Model -> Protobuf
        var reply = ProtoLookupReply.newBuilder()
            .setUrl(response.url())
            .build();

        // 4. Send gRPC response
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }


    @Override
    public void heartbeat(ProtoVolumeBeatRequest protoRequest,
                          StreamObserver<ProtoVolumeBeatReply> responseObserver) {

        // 1. Map Protobuf -> Domain Model
        var request = new VolumeBeatRequest(
            protoRequest.getUrl(),
            protoRequest.getCount(),
            protoRequest.getVinfoList().stream()
                .map(item -> new VolumeInfo(
                    item.getVid(),
                    item.getFileCount(),
                    item.getAvailableSize(),
                    item.getStatus())).toList()
        );

        // 2. Call the shared logic
        var response = manager.onHeartbeat(request);
        logger.debug("heartbeat url={} volumes={}", request.url(), request.count());

        // 3. Map Domain Model -> Protobuf
        var reply = ProtoVolumeBeatReply.newBuilder()
            .setStatus(response.status())
            .build();

        // 4. Send gRPC response
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void state(ProtoStateRequest protoRequest, StreamObserver<ProtoStateReply> responseObserver) {
        var response = manager.onState();
        logger.info("state → nodes={} volumes={}", response.nodes().size(), response.volumes().size());

        var reply = ProtoStateReply.newBuilder()
            .addAllNodes(response.nodes().stream()
                .map(node -> ProtoStateNode.newBuilder()
                    .setUrl(node.url())
                    .setOnline(node.online())
                    .setLastSeen(node.lastSeen())
                    .build())
                .toList())
            .addAllVolumes(response.volumes().stream()
                .map(volume -> ProtoStateVolume.newBuilder()
                    .setVid(volume.vid())
                    .setNodeUrl(volume.nodeUrl())
                    .setFileCount(volume.fileCount())
                    .setAvailableSize(volume.availableSize())
                    .setStatus(volume.status())
                    .build())
                .toList())
            .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }


}
