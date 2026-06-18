package deti.sd.moss.core.manager;

import java.time.Instant;
import java.util.Set;

public record NodeRecord(String url, Instant lastSeen, Set<Integer> vids) {}
