package deti.sd.moss.core.object.model;

public record PutRequest(String bucket, String path, byte[] data){}

