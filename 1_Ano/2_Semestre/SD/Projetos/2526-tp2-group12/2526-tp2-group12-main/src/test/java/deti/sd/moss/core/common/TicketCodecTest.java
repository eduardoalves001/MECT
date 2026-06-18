// Tests generated with AI assistance
package deti.sd.moss.core.common;

import deti.sd.moss.core.common.model.TicketInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketCodecTest {

    @Test
    void create_formatsVidFidAndCookieCorrectly() {
        String ticket = TicketCodec.create(3, 42, 0xdeadbeef);
        assertEquals("3:42deadbeef", ticket);
    }

    @Test
    void create_zeropadsCookieTo8Chars() {
        String ticket = TicketCodec.create(1, 1, 0x000000ff);
        assertTrue(ticket.endsWith("000000ff"));
    }

    @Test
    void parse_extractsVidFidAndCookie() {
        TicketInfo info = TicketCodec.parse("3:42deadbeef");
        assertEquals(3, info.vid());
        assertEquals(42, info.fid());
        assertEquals(0xdeadbeef, info.cookie());
    }

    @Test
    void createAndParse_roundtrip() {
        int vid = 7, fid = 1234, cookie = 0xabcd1234;
        TicketInfo info = TicketCodec.parse(TicketCodec.create(vid, fid, cookie));
        assertEquals(vid, info.vid());
        assertEquals(fid, info.fid());
        assertEquals(cookie, info.cookie());
    }

    @Test
    void generateCookie_returnsDifferentValuesOnRepetition() {
        int a = TicketCodec.generateCookie();
        int b = TicketCodec.generateCookie();
        int c = TicketCodec.generateCookie();
        assertFalse(a == b && b == c, "All three cookies are equal — likely not random");
    }
}
