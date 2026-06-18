package deti.sd.moss.core.common.ports;

import deti.sd.moss.core.manager.ports.ManagerService;
import deti.sd.moss.core.volume.ports.VolumeService;
import deti.sd.moss.core.object.ports.ObjectService;

public interface ServiceDiscovery {
    public ManagerService getManager(String name);
    public ObjectService getObject(String name);
    public VolumeService getVolume(String name);
}

