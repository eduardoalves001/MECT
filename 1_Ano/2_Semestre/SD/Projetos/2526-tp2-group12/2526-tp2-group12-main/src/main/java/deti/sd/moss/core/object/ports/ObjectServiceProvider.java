package deti.sd.moss.core.object.ports;

import deti.sd.moss.core.object.model.*;

public interface ObjectServiceProvider {
    public GetReply onGet(GetRequest request);
    public PutReply onPut(PutRequest request);
    public ListReply onList(ListRequest request);
    public RemoveReply onRemove(RemoveRequest request);
}
