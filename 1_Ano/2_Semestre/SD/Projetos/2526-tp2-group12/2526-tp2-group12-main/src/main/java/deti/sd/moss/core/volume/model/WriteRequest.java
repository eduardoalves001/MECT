package deti.sd.moss.core.volume.model;

public record WriteRequest(int vid, int fid, int cookie, byte[] data){}
