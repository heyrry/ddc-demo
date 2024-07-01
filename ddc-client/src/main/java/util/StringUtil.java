package util;

/**
 * @author baofeng
 * @date 2023/11/19
 */
public class StringUtil {
    public static String getString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number) {
            return value.toString();
        }
        return null;
    }
}
