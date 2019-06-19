package uk.newcastle.jiajie.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Decode byte package from the band
 */
public class DecodeUtil {

    /**
     * Decode bytes
     */
    public static String decodeBytes(byte[] inBytes) {
        if (inBytes.length < 16 && (inBytes.length - 18) % 12 != 0) {
            return "Decode fail" + new String(inBytes);
        }
        byte[] bytes = buildBytesFromHex(inBytes);
        int timestamp = (bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8 |
                (bytes[2] & 0xFF) << 16 | (bytes[3] & 0xFF) << 24;
        int size = (bytes.length - 8) / 6;
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.00");
        sb.append(df.format(timestamp/32768.)).append(" | ");
        for (int i = 0; i < size; i++) {
            int x = (bytes[i * 6 + 8] & 0xFF) |
                    (bytes[i * 6 + 8 + 1] & 0xFF) << 8;
            int y = (bytes[i * 6 + 8 + 2] & 0xFF) |
                    (bytes[i * 6 + 8 + 3] & 0xFF) << 8;
            int z = (bytes[i * 6 + 8 + 4] & 0xFF) |
                    (bytes[i * 6 + 8 + 5] & 0xFF) << 8;
            sb
                    .append("x:").append(x).append(',')
                    .append("y:").append(y).append(',')
                    .append("z:").append(z).append(';');
        }
        return sb.toString();
    }

    public static void printBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.valueOf(b & 0xff)).append(" ");
        }
        System.out.println(sb.toString());
    }

    /**
     * Build byte from 2 hex chars
     */
    private static byte[] buildBytesFromHex(byte[] inBytes) {
        byte[] ret = new byte[inBytes.length / 2];
        for (int i = 0; i < inBytes.length / 2; i++) {
            ret[i] = (byte) ((Character.digit(inBytes[2 * i], 16) << 4)
                    + Character.digit(inBytes[2 * i + 1], 16));
        }
        return ret;
    }

    public static String decodeBytes(String s) {
        return decodeBytes(s.getBytes());
    }

    public static void main(String[] args) {
        System.out.println(decodeBytes("BE8703001E8075007700BB1015007D00B41011007800AB1009007300B11006008300AB100A009100A51015008C00B01029008600B6102F008000B8101F007D00BD1016007F00BC101C007400B91020007600BF101E008100C0101F008600B91017008400B7100C008500AC1007008400A81015007D00A71020007F00A01021007E00A51028007B00A81025007C00B1101E008000AC102B008100A7103900\r\n"));
    }
}
