package deti.sd.moss.infra.rpc.volume;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deti.sd.moss.infra.rpc.volume.VolumeGrpc;
import deti.sd.moss.infra.rpc.volume.ProtoAssignVolumeRequest;
import deti.sd.moss.infra.rpc.volume.ProtoAssignVolumeReply;
import deti.sd.moss.infra.rpc.volume.ProtoWriteRequest;
import deti.sd.moss.infra.rpc.volume.ProtoWriteReply;
import deti.sd.moss.infra.rpc.volume.ProtoReadRequest;
import deti.sd.moss.infra.rpc.volume.ProtoReadReply;
import deti.sd.moss.infra.rpc.volume.ProtoDeleteRequest;
import deti.sd.moss.infra.rpc.volume.ProtoDeleteReply;
import deti.sd.moss.infra.rpc.volume.ProtoCompactRequest;
import deti.sd.moss.infra.rpc.volume.ProtoCompactReply;

import deti.sd.moss.core.volume.ports.VolumeServiceProvider;
import deti.sd.moss.core.volume.model.*;
import deti.sd.moss.core.volume.model.DeleteRequest;
import deti.sd.moss.core.volume.model.DeleteReply;
import deti.sd.moss.core.volume.model.CompactRequest;
import deti.sd.moss.core.volume.model.CompactReply;

public final class VolumeGrpcService extends VolumeGrpc.VolumeImplBase {

    private static final Logger logger = LoggerFactory.getLogger(VolumeGrpcService.class);

    private final VolumeServiceProvider provider;

    public VolumeGrpcService(VolumeServiceProvider provider){
        this.provider = provider;
    }

    @Override
    public void assignVolume(ProtoAssignVolumeRequest protoRequest,
                    StreamObserver<ProtoAssignVolumeReply> responseObserver) {

        // 1. Map Protobuf -> Domain Model
        var request = new AssignVolumeRequest(
            protoRequest.getVid(),
            protoRequest.getUrlsList()
        );

        // 2. Call the shared logic
        var response = provider.onAssignVolume(request);
        logger.info("assignVolume vid={} → status={}", request.vid(), response.status());

        // 3. Map Domain Model -> Protobuf
        var reply = ProtoAssignVolumeReply.newBuilder()
            .setStatus(response.status())
            .build();

        // 4. Send gRPC response
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void write(ProtoWriteRequest protoRequest,
                    StreamObserver<ProtoWriteReply> responseObserver) {

        // 1. Map Protobuf -> Domain Model
        var request = new WriteRequest(
            protoRequest.getVid(),
            protoRequest.getFid(),
            protoRequest.getCookie(),
            protoRequest.getData().toByteArray()
        );

        // 2. Call the shared logic
        var response = provider.onWrite(request);
        logger.info("write vid={} fid={} size={} → status={}", request.vid(), request.fid(), request.data().length, response.status());

        // 3. Map Domain Model -> Protobuf
        var reply = ProtoWriteReply.newBuilder()
            .setStatus(response.status())
            .build();

        // 4. Send gRPC response
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void read(ProtoReadRequest protoRequest,
                    StreamObserver<ProtoReadReply> responseObserver) {

        var request = new ReadRequest(
            protoRequest.getVid(),
            protoRequest.getFid(),
            protoRequest.getCookie()
        );

        var response = provider.onRead(request);
        logger.info("read vid={} fid={} → status={}", request.vid(), request.fid(), response.status());

        var reply = ProtoReadReply.newBuilder()
            .setStatus(response.status())
            .setData(ByteString.copyFrom(response.data()))
            .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void delete(ProtoDeleteRequest protoRequest, StreamObserver<ProtoDeleteReply> responseObserver) {
        var request = new DeleteRequest(protoRequest.getVid(), protoRequest.getFid(), protoRequest.getCookie());
        var response = provider.onDelete(request);
        logger.info("delete vid={} fid={} → status={}", request.vid(), request.fid(), response.status());
        responseObserver.onNext(ProtoDeleteReply.newBuilder().setStatus(response.status()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void compact(ProtoCompactRequest protoRequest, StreamObserver<ProtoCompactReply> responseObserver) {
        var request = new CompactRequest(protoRequest.getVid());
        var response = provider.onCompact(request);
        logger.info("compact vid={} → status={}", request.vid(), response.status());
        responseObserver.onNext(ProtoCompactReply.newBuilder().setStatus(response.status()).build());
        responseObserver.onCompleted();
    }

}
