package deti.sd.moss.infra.rpc.volume;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;

import deti.sd.moss.util.StubRunner;

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

import deti.sd.moss.core.volume.ports.VolumeService;
import deti.sd.moss.core.volume.model.*;

public class VolumeGrpcAdapter implements VolumeService {
    private final VolumeGrpc.VolumeBlockingStub stub;

    public VolumeGrpcAdapter(ManagedChannel channel) {
        this.stub = VolumeGrpc.newBlockingStub(channel);
    }

    @Override
    public AssignVolumeReply assignVolume(AssignVolumeRequest request){

        // 1. Map Domain Model -> Protobuf
        var protoRequest = ProtoAssignVolumeRequest.newBuilder()
            .setVid(request.vid())
            .addAllUrls(request.url())
            .build();

        // 2. Make the call
        var response = StubRunner.execute(() -> stub.assignVolume(protoRequest))
            .orFail(() -> {
                System.out.println("Failed to connect");
                return ProtoAssignVolumeReply.getDefaultInstance();
            });

        // 3. Map Protobuf -> Domain Model
        return new AssignVolumeReply(
            response.getStatus()
        );
    }

    @Override
    public WriteReply write(WriteRequest request) {

        // 1. Map Domain Model -> Protobuf
        var protoRequest = ProtoWriteRequest.newBuilder()
            .setVid(request.vid())
            .setFid(request.fid())
            .setCookie(request.cookie())
            .setData(ByteString.copyFrom(request.data()))
            .build();

        // 2. Make the call
        var response = StubRunner.execute(() -> stub.write(protoRequest))
            .orFail(() -> {
                System.out.println("Failed to connect");
                return ProtoWriteReply.getDefaultInstance();
            });

        // 3. Map Protobuf -> Domain Model
        return new WriteReply(
            response.getStatus()
        );
    }

    @Override
    public ReadReply read(ReadRequest request) {

        var protoRequest = ProtoReadRequest.newBuilder()
            .setVid(request.vid())
            .setFid(request.fid())
            .setCookie(request.cookie())
            .build();

        var response = StubRunner.execute(() -> stub.read(protoRequest))
            .orFail(() -> {
                System.out.println("Failed to connect");
                return ProtoReadReply.getDefaultInstance();
            });

        return new ReadReply(
            response.getStatus(),
            response.getData().toByteArray()
        );
    }

    @Override
    public DeleteReply delete(DeleteRequest request) {
        var protoRequest = ProtoDeleteRequest.newBuilder()
            .setVid(request.vid())
            .setFid(request.fid())
            .setCookie(request.cookie())
            .build();
        var response = StubRunner.execute(() -> stub.delete(protoRequest))
            .orFail(() -> {
                System.out.println("Failed to connect");
                return ProtoDeleteReply.getDefaultInstance();
            });
        return new DeleteReply(response.getStatus());
    }

    @Override
    public CompactReply compact(CompactRequest request) {
        var protoRequest = ProtoCompactRequest.newBuilder()
            .setVid(request.vid())
            .build();
        var response = StubRunner.execute(() -> stub.compact(protoRequest))
            .orFail(() -> {
                System.out.println("Failed to connect");
                return ProtoCompactReply.getDefaultInstance();
            });
        return new CompactReply(response.getStatus());
    }

}

