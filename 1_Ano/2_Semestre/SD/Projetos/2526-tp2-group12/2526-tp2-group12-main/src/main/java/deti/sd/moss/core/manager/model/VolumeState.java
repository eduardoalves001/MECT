package deti.sd.moss.core.manager.model;

public record VolumeState(int vid, String nodeUrl, int fileCount, int availableSize, int status) {}