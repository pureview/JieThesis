package uk.newcastle.jiajie.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Decode byte package from the band
 */
public class DecodeUtil {

    /**
     * Decode bytes
     */
    public static String decodeBytes(byte[] bytes) {
        int prefixSize = 8, phaseSize = 6;
        if (bytes.length < prefixSize + phaseSize) {
            return "Decode fail, too short" + new String(bytes);
        }
        int timestamp = ((int) bytes[0]) | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24);
        timestamp/=32768;
        Date date = new Date(timestamp*1000);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr=dateFormat.format(date);
        int size = (bytes.length - prefixSize) / phaseSize;
        StringBuilder sb = new StringBuilder();
        sb.append(dateStr).append(" | ");
        for (int i = 0; i < size; i++) {
            int x = ((int) bytes[i * phaseSize + prefixSize]) |
                    ((int) bytes[i * phaseSize + prefixSize + 1]) << 8;
            int y = ((int) bytes[i * phaseSize + prefixSize + 2]) |
                    ((int) bytes[i * phaseSize + prefixSize + 3]) << 8;
            int z = ((int) bytes[i * phaseSize + prefixSize + 4]) |
                    ((int) bytes[i * phaseSize + prefixSize + 5]) << 8;
            sb
                    .append("x:").append(x).append(',')
                    .append("y:").append(y).append(',')
                    .append("z:").append(z).append(';');
        }
        return sb.toString();
    }

    public static String decodeBytes(String s) {
        return decodeBytes(s.getBytes());
    }

    public static void main(String[] args) {
        System.out.println(decodeBytes("3400441097FF30003410"));
    }
}
