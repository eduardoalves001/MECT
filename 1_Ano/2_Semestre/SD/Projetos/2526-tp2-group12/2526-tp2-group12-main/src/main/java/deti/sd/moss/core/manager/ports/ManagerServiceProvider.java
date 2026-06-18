package deti.sd.moss.core.manager.ports;

import deti.sd.moss.core.manager.model.*;

public interface ManagerServiceProvider {
    public AssignReply onAssign(AssignRequest request);
    public LookupReply onLookup(LookupRequest request);
    public VolumeBeatReply onHeartbeat(VolumeBeatRequest request);
    public StateReply onState();
}
