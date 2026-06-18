// Tests generated with AI assistance
package deti.sd.moss.core.volume;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VolumeWriteTest {

    @TempDir
    Path tempDir;

    private Volume volume;

    @BeforeEach
    void setUp() throws IOException {
        volume = new Volume(1, tempDir.toString());
    }

    @Test
    void writeSucceedsAndUpdatesIndex() throws IOException {
        WriteResult result = volume.write(42, 0xdeadbeef, new byte[]{1, 2, 3});
        assertEquals(WriteResult.OK, result);
        assertTrue(volume.getIndex().containsKey(42));
    }

    @Test
    void writeDuplicateFidFails() throws IOException {
        volume.write(42, 0xdeadbeef, new byte[]{1, 2, 3});
        WriteResult result = volume.write(42, 0xdeadbeef, new byte[]{4, 5, 6});
        assertEquals(WriteResult.DUPLICATE, result);
    }

    @Test
    void writeUpdatesUsedBytes() throws IOException {
        byte[] data = new byte[1024];
        volume.write(1, 111, data);
        assertEquals(1024, volume.getUsedBytes().get());
    }

    @Test
    void writeReturnsFullWhenCapacityExceeded() throws IOException {
        // Fill up to just under the limit first by writing large chunks
        byte[] chunk = new byte[Volume.MAX_VOLUME_SIZE - 10];
        volume.write(1, 1, chunk);

        WriteResult result = volume.write(2, 2, new byte[100]);
        assertEquals(WriteResult.FULL, result);
    }

    @Test
    void writePersistsDataToDisk() throws IOException {
        byte[] data = "hello".getBytes();
        volume.write(10, 999, data);

        // Reload from disk and verify the entry is present
        volume.reload();
        assertTrue(volume.getIndex().containsKey(10));
        assertEquals(data.length, volume.getIndex().get(10).size());
    }

    @Test
    void writeMultipleFidsSucceed() throws IOException {
        volume.write(1, 111, new byte[]{1});
        volume.write(2, 222, new byte[]{2});
        volume.write(3, 333, new byte[]{3});

        assertEquals(3, volume.getIndex().size());
    }
}
