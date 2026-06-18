package deti.sd.moss.core.volume.ports;

import deti.sd.moss.core.volume.model.*;

public interface VolumeService {
    public AssignVolumeReply assignVolume(AssignVolumeRequest request);
    public WriteReply write(WriteRequest request);
    public ReadReply read(ReadRequest request);
    public DeleteReply delete(DeleteRequest request);
    public CompactReply compact(CompactRequest request);
}
