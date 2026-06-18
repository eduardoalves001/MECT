package deti.sd.moss.core.object;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.h2.mvstore.type.StringDataType; // Import this!
import org.h2.mvstore.DataUtils;


public class ObjectEntryDataType extends BasicDataType<ObjectEntry> {

    @Override
    public void write(WriteBuffer buff, ObjectEntry obj) {
        // 1. List Size
        buff.putVarInt(obj.ticket().size());
        // 2. Strings - Use StringDataType.INSTANCE
        for (String s : obj.ticket()) {
            StringDataType.INSTANCE.write(buff, s);
        }
        // 3. Instant
        buff.putLong(obj.timestamp().getEpochSecond());
        buff.putInt(obj.timestamp().getNano());
        // 4. Size
        buff.putInt(obj.size());
    }

    @Override
    public ObjectEntry read(ByteBuffer buff) {
        // 1. Read List Size using DataUtils
        int listSize = DataUtils.readVarInt(buff);

        List<String> tickets = new ArrayList<>(listSize);
        // 2. Strings - Use StringDataType.INSTANCE
        for (int i = 0; i < listSize; i++) {
            tickets.add(StringDataType.INSTANCE.read(buff));
        }
        // 3. Instant
        long seconds = buff.getLong();
        int nanos = buff.getInt();
        // 4. Size
        int size = buff.getInt();

        return new ObjectEntry(tickets, Instant.ofEpochSecond(seconds, nanos), size);
    }

    @Override
    public int getMemory(ObjectEntry obj) {
        // Memory estimate for the cache (roughly)
        int stringMem = obj.ticket().stream().mapToInt(s -> s.length() * 2 + 24).sum();
        return 24 + stringMem + 12 + 4; // List overhead + strings + instant + int
    }

    @Override
    public ObjectEntry[] createStorage(int size) {
        return new ObjectEntry[size];
    }
}
