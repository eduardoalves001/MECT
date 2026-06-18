package deti.sd.moss.core.manager.model;

import java.util.List;

import deti.sd.moss.core.common.model.VolumeInfo;

public record VolumeBeatRequest(
    String url,
    int count,
    List<VolumeInfo> vinfo
){}

