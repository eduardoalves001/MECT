package deti.sd.moss.core.manager.ports;

import deti.sd.moss.core.manager.model.*;

public interface ManagerService {
    public AssignReply assign(AssignRequest request);
    public LookupReply lookup(LookupRequest request);
    public VolumeBeatReply heartbeat(VolumeBeatRequest request);
    public StateReply state();
}
