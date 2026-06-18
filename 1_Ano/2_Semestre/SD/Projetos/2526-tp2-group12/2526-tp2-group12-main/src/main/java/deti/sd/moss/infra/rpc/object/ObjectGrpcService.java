package deti.sd.moss.infra.rpc.object;

import io.grpc.stub.StreamObserver;

import com.google.protobuf.ByteString;

import deti.sd.moss.infra.rpc.object.ObjectGrpc;
import deti.sd.moss.infra.rpc.object.ProtoPutRequest;
import deti.sd.moss.infra.rpc.object.ProtoPutReply;
import deti.sd.moss.infra.rpc.object.ProtoGetRequest;
import deti.sd.moss.infra.rpc.object.ProtoGetReply;
import deti.sd.moss.infra.rpc.object.ProtoListRequest;
import deti.sd.moss.infra.rpc.object.ProtoListReply;
import deti.sd.moss.infra.rpc.object.ProtoRemoveRequest;
import deti.sd.moss.infra.rpc.object.ProtoRemoveReply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deti.sd.moss.core.object.ports.ObjectServiceProvider;
import deti.sd.moss.core.object.model.*;
import deti.sd.moss.core.object.model.RemoveRequest;
import deti.sd.moss.core.object.model.RemoveReply;

public final class ObjectGrpcService extends ObjectGrpc.ObjectImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ObjectGrpcService.class);

    private final ObjectServiceProvider manager;

    public ObjectGrpcService(ObjectServiceProvider manager){
        this.manager = manager;
    }

    @Override
    public void put(ProtoPutRequest protoRequest, StreamObserver<ProtoPutReply> responseObserver) {
        // 1. Map Protobuf -> Domain Model
        var request = new PutRequest(
            protoRequest.getBucket(),
            protoRequest.getPath(),
            protoRequest.getData().toByteArray()
        );

        // 2. Call the shared logic
        var response = manager.onPut(request);
        logger.info("put bucket={} path={} size={} → status={}", request.bucket(), request.path(), request.data().length, response.status());

        // 3. Map Domain Model -> Protobuf
        var reply = ProtoPutReply.newBuilder()
            .setStatus(response.status())
            .build();

        // 4. Send gRPC response
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void get(ProtoGetRequest protoRequest, StreamObserver<ProtoGetReply> responseObserver) {

        var request = new GetRequest(
            protoRequest.getBucket(),
            protoRequest.getPath()
        );

        var response = manager.onGet(request);
        logger.info("get bucket={} path={} → status={} size={}", request.bucket(), request.path(), response.status(), response.data().length);

        var reply = ProtoGetReply.newBuilder()
            .setStatus(response.status())
            .setData(ByteString.copyFrom(response.data()))
            .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void list(ProtoListRequest protoRequest, StreamObserver<ProtoListReply> responseObserver) {

        var request = new ListRequest(protoRequest.getBucket());

        var response = manager.onList(request);
        logger.info("list bucket={} → count={} status={}", request.bucket(), response.objects().size(), response.status());

        var replyBuilder = ProtoListReply.newBuilder()
            .setStatus(response.status());

        for (var obj : response.objects()) {
            replyBuilder.addObjects(ProtoListReply.ProtoObjectInfo.newBuilder()
                .setPath(obj.path())
                .setSize(obj.size())
                .setTimestamp(obj.timestamp())
                .build());
        }

        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void remove(ProtoRemoveRequest protoRequest, StreamObserver<ProtoRemoveReply> responseObserver) {
        var request = new RemoveRequest(protoRequest.getBucket(), protoRequest.getPath());
        var response = manager.onRemove(request);
        logger.info("remove bucket={} path={} → status={}", request.bucket(), request.path(), response.status());
        responseObserver.onNext(ProtoRemoveReply.newBuilder().setStatus(response.status()).build());
        responseObserver.onCompleted();
    }

}
