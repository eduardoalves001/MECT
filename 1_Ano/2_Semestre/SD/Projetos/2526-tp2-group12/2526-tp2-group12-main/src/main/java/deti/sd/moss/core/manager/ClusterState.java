package deti.sd.moss.core.manager;

import java.util.List;

import deti.sd.moss.core.manager.model.NodeState;
import deti.sd.moss.core.manager.model.VolumeState;

public record ClusterState(List<NodeState> nodes, List<VolumeState> volumes) {}