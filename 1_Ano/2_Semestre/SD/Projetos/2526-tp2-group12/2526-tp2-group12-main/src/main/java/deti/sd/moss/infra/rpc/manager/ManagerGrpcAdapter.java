package deti.sd.moss.infra.rpc.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;

import deti.sd.moss.util.StubRunner;

import deti.sd.moss.infra.rpc.manager.ManagerGrpc;
import deti.sd.moss.infra.rpc.manager.ProtoAssignRequest;
import deti.sd.moss.infra.rpc.manager.ProtoAssignReply;
import deti.sd.moss.infra.rpc.manager.ProtoLookupRequest;
import deti.sd.moss.infra.rpc.manager.ProtoLookupReply;
import deti.sd.moss.infra.rpc.manager.ProtoVolumeBeatRequest;
import deti.sd.moss.infra.rpc.manager.ProtoVolumeBeatReply;
import deti.sd.moss.infra.rpc.manager.ProtoStateRequest;
import deti.sd.moss.infra.rpc.manager.ProtoStateReply;

import deti.sd.moss.core.manager.ports.ManagerService;
import deti.sd.moss.core.manager.model.*;

import deti.sd.moss.core.volume.model.AssignVolumeRequest;
import deti.sd.moss.core.volume.model.AssignVolumeReply;

public class ManagerGrpcAdapter implements ManagerService {
    private static final Logger logger = LoggerFactory.getLogger(ManagerGrpcAdapter.class);

    private final ManagerGrpc.ManagerBlockingStub stub;

    public ManagerGrpcAdapter(ManagedChannel channel) {
        this.stub = ManagerGrpc.newBlockingStub(channel);
    }

    @Override
    public AssignReply assign(AssignRequest request){
        // 1. Map Domain Model -> Protobuf
        ProtoAssignRequest protoRequest = ProtoAssignRequest.newBuilder()
            .setReplicas(request.replicas())
            .build();

        // 2. Make the call
        ProtoAssignReply response = StubRunner.execute(() -> stub.assign(protoRequest))
            .orFail(() -> {
                logger.error("Failed to connect to manager node");
                return ProtoAssignReply.getDefaultInstance();
            });

        // 3. Map Protobuf -> Domain Model
        return new AssignReply(
            response.getTicket(),
            response.getVolumeUrl(),
            response.getCount(),
            response.getUrlList()
        );
    }

    @Override
    public LookupReply lookup(LookupRequest request){
        // 1. Map Domain Model -> Protobuf
        ProtoLookupRequest protoRequest = ProtoLookupRequest.newBuilder()
            .setTicket(request.ticket())
            .build();

        // 2. Make the call
        ProtoLookupReply response = StubRunner.execute(() -> stub.lookup(protoRequest))
            .orFail(() -> {
                logger.error("Failed to connect to manager node");
                return ProtoLookupReply.getDefaultInstance();
            });

        // 3. Map Protobuf -> Domain Model
        return new LookupReply(response.getUrl());
    }

    @Override
    public VolumeBeatReply heartbeat(VolumeBeatRequest request) {
        // 1. Map Domain Model -> Protobuf
        ProtoVolumeBeatRequest protoRequest = ProtoVolumeBeatRequest.newBuilder()
            .setUrl(request.url())
            .setCount(request.count())
            .addAllVinfo(request.vinfo().stream()
                .map(item -> ProtoVolumeBeatRequest.ProtoVolumeInfo.newBuilder()
                    .setVid(item.vid())
                    .setFileCount(item.fileCount())
                    .setAvailableSize(item.availableSize())
                    .build()
                ).toList())
            .build();

        // 2. Make the call
        ProtoVolumeBeatReply response = StubRunner.execute(() -> stub.heartbeat(protoRequest))
            .orFail(() -> {
                logger.error("Failed to connect to manager node");
                return ProtoVolumeBeatReply.getDefaultInstance();
            });

        // 3. Map Protobuf -> Domain Model
        return new VolumeBeatReply(response.getStatus());
    }

    @Override
    public StateReply state() {
        var protoRequest = ProtoStateRequest.getDefaultInstance();

        var response = StubRunner.execute(() -> stub.state(protoRequest))
            .orFail(() -> {
                logger.error("Failed to connect to manager node");
                return ProtoStateReply.getDefaultInstance();
            });

        return new StateReply(
            response.getNodesList().stream()
                .map(node -> new NodeState(node.getUrl(), node.getOnline(), node.getLastSeen()))
                .toList(),
            response.getVolumesList().stream()
                .map(volume -> new VolumeState(
                    volume.getVid(),
                    volume.getNodeUrl(),
                    volume.getFileCount(),
                    volume.getAvailableSize(),
                    volume.getStatus()))
                .toList()
        );
    }


}

