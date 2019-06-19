package uk.newcastle.jiajie.util;

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
        if(inBytes.length < 16 && (inBytes.length - 18) % 12 != 0){
            return "Decode fail"+ new String(inBytes);
        }
        byte[] bytes=buildBytesFromHex(inBytes);
        int timestamp = ((int) bytes[0]) | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24);
        timestamp/=32768;
        Date date = new Date(timestamp*1000);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr=dateFormat.format(date);
        int size = (bytes.length - 8) / 6;
        StringBuilder sb = new StringBuilder();
        sb.append(dateStr).append(" | ");
        for (int i = 0; i < size; i++) {
            int x = ((int) bytes[i * 6 + 8]) |
                    ((int) bytes[i * 6 + 8 + 1]) << 8;
            int y = ((int) bytes[i * 6 + 8 + 2]) |
                    ((int) bytes[i * 6 + 8 + 3]) << 8;
            int z = ((int) bytes[i * 6 + 8 + 4]) |
                    ((int) bytes[i * 6 + 8 + 5]) << 8;
            sb
                    .append("x:").append(x).append(',')
                    .append("y:").append(y).append(',')
                    .append("z:").append(z).append(';');
        }
        return sb.toString();
    }

    /**
     * Build byte from 2 hex chars
     */
    private static byte[] buildBytesFromHex(byte[] inBytes) {
        byte[] ret=new byte[inBytes.length/2];
        for(int i=0;i<inBytes.length/2;i++){
            ret[i]=(byte) ((Character.digit(inBytes[2*i], 16) << 4)
                    + Character.digit(inBytes[2*i+1], 16));
        }
        return ret;
    }

    public static String decodeBytes(String s) {
        return decodeBytes(s.getBytes());
    }

    public static void main(String[] args) {
        System.out.println(new String(buildBytesFromHex("30".getBytes())));
    }
}
