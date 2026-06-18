package deti.sd.moss.infra.rpc.object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;

import deti.sd.moss.util.StubRunner;

import deti.sd.moss.infra.rpc.object.ObjectGrpc;
import deti.sd.moss.infra.rpc.object.ProtoPutRequest;
import deti.sd.moss.infra.rpc.object.ProtoPutReply;
import deti.sd.moss.infra.rpc.object.ProtoGetRequest;
import deti.sd.moss.infra.rpc.object.ProtoGetReply;
import deti.sd.moss.infra.rpc.object.ProtoListRequest;
import deti.sd.moss.infra.rpc.object.ProtoListReply;
import deti.sd.moss.infra.rpc.object.ProtoRemoveRequest;
import deti.sd.moss.infra.rpc.object.ProtoRemoveReply;

import deti.sd.moss.core.object.ports.ObjectService;
import deti.sd.moss.core.object.model.*;
import deti.sd.moss.core.object.model.RemoveRequest;
import deti.sd.moss.core.object.model.RemoveReply;

public class ObjectGrpcAdapter implements ObjectService {
    private static final Logger logger = LoggerFactory.getLogger(ObjectGrpcAdapter.class);

    private final ObjectGrpc.ObjectBlockingStub stub;

    public ObjectGrpcAdapter(ManagedChannel channel) {
        this.stub = ObjectGrpc.newBlockingStub(channel);
    }

    @Override
    public PutReply put(PutRequest request){

        // 1. Map Domain Model -> Protobuf
        ProtoPutRequest protoRequest = ProtoPutRequest.newBuilder()
            .setBucket(request.bucket())
            .setPath(request.path())
            .setData(ByteString.copyFrom(request.data()))
            .build();

        // 2. Make the call
        ProtoPutReply response = StubRunner.execute(() -> stub.put(protoRequest))
            .orFail(() -> {
                logger.error("gRPC conneection failed");
                return ProtoPutReply.getDefaultInstance();
            });

        // 3. Map Protobuf -> Domain Model
        return new PutReply(
            response.getStatus()
        );
    }

    @Override
    public GetReply get(GetRequest request) {

        var protoRequest = ProtoGetRequest.newBuilder()
            .setBucket(request.bucket())
            .setPath(request.path())
            .build();

        var response = StubRunner.execute(() -> stub.get(protoRequest))
            .orFail(() -> {
                logger.error("gRPC connection failed");
                return ProtoGetReply.getDefaultInstance();
            });

        return new GetReply(
            response.getStatus(),
            response.getData().toByteArray()
        );
    }

    @Override
    public ListReply list(ListRequest request) {

        var protoRequest = ProtoListRequest.newBuilder()
            .setBucket(request.bucket())
            .build();

        var response = StubRunner.execute(() -> stub.list(protoRequest))
            .orFail(() -> {
                logger.error("gRPC connection failed");
                return ProtoListReply.getDefaultInstance();
            });

        var objects = response.getObjectsList().stream()
            .map(obj -> new ListReply.ObjectInfo(
                obj.getPath(),
                obj.getSize(),
                obj.getTimestamp()))
            .toList();

        return new ListReply(response.getStatus(), objects);
    }

    @Override
    public RemoveReply remove(RemoveRequest request) {
        var protoRequest = ProtoRemoveRequest.newBuilder()
            .setBucket(request.bucket())
            .setPath(request.path())
            .build();
        var response = StubRunner.execute(() -> stub.remove(protoRequest))
            .orFail(() -> {
                logger.error("gRPC connection failed");
                return ProtoRemoveReply.getDefaultInstance();
            });
        return new RemoveReply(response.getStatus());
    }

}

