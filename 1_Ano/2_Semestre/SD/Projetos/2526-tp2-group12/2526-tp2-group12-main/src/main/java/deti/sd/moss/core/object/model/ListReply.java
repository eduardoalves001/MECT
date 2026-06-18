package deti.sd.moss.core.object.model;

import java.util.List;

public record ListReply(int status, List<ObjectInfo> objects) {

    public record ObjectInfo(String path, int size, long timestamp) {}
}
