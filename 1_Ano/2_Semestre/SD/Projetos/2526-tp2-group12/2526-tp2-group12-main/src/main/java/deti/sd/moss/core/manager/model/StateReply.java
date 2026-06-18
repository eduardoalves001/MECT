package deti.sd.moss.core.manager.model;

import java.util.List;

public record StateReply(List<NodeState> nodes, List<VolumeState> volumes) {}