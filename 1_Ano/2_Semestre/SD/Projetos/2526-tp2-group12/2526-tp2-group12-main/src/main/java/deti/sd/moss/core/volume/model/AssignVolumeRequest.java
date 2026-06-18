package deti.sd.moss.core.volume.model;

import java.util.List;

public record AssignVolumeRequest(
    int vid,
    List<String> url
){}
