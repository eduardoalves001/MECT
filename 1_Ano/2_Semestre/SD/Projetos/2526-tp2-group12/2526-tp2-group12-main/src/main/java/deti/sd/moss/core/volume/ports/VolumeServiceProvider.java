package deti.sd.moss.core.volume.ports;

import deti.sd.moss.core.volume.model.*;

public interface VolumeServiceProvider {
    public AssignVolumeReply onAssignVolume(AssignVolumeRequest request);
    public WriteReply onWrite(WriteRequest request);
    public ReadReply onRead(ReadRequest request);
    public DeleteReply onDelete(DeleteRequest request);
    public CompactReply onCompact(CompactRequest request);
    public void shutdown();
}
