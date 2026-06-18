package deti.sd.moss.core.common;

import java.util.concurrent.ThreadLocalRandom;

import deti.sd.moss.core.common.model.TicketInfo;

public final class TicketCodec {

    private TicketCodec() {}

    public static String create(int vid, int fid, int cookie) {
        return vid + ":" + fid + String.format("%08x", cookie);
    }

    public static TicketInfo parse(String ticket) {
        String[] parts = ticket.split(":");
        int vid = Integer.parseInt(parts[0]);
        String second = parts[1];
        String cookieHex = second.substring(second.length() - 8);
        int fid = Integer.parseInt(second.substring(0, second.length() - 8));
        int cookie = (int) Long.parseLong(cookieHex, 16);
        return new TicketInfo(vid, fid, cookie);
    }

    public static int generateCookie() {
        return ThreadLocalRandom.current().nextInt();
    }
}
