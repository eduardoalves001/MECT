package deti.sd.moss.core.object;

import java.util.List;
import java.time.Instant;

// Example of an object metadata

public record ObjectEntry(
    // If replication exists, the object can have multiple tickets
    List<String> ticket,
    Instant timestamp,
    int size
) {}
