package uk.newcastle.jiajie.util;

public class StringUtil {

    public static boolean isNum(char c) {
        return c <= '9' && c >= '0';
    }

    public static String escap(String raw) {
        StringBuilder sb = new StringBuilder();
        StringBuilder t;
        int i = 0;
        while (i < raw.length() - 1) {
            if (raw.charAt(i) == '\\' && isNum(raw.charAt(i + 1))) {
                i += 1;
                t = new StringBuilder();
                while (i < raw.length() && isNum(raw.charAt(i))) {
                    t.append(raw.charAt(i));
                    i++;
                }
                sb.append(Character.forDigit(Integer.valueOf(t.toString()), 10));
            } else {
                sb.append(raw.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }
}
