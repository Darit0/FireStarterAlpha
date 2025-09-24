package interntask.fileprocessor.util;

//штука для правки JSON
public class JsonUtil {
    public static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}