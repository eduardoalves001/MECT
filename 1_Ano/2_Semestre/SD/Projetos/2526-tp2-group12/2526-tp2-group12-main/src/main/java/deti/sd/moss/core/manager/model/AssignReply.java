package deti.sd.moss.core.manager.model;

import java.util.List;

public record AssignReply(
  // The File ticket. The format is <Volume ID>:<FileKey><Cookie>
  String ticket,
  // The address of the volume node
  String volumeUrl,
  // Number of replicas. Ingored if replicas is not implemented
  int count,
  // The addresses of the volume nodes for replication
  List<String> url
){}
