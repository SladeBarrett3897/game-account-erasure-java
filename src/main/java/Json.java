import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private final String text;
    private int index;
    private Json(String text) { this.text = text; }

    static Map<String, Object> parseObject(String text) {
        Object value = new Json(text).parseDocument();
        if (!(value instanceof Map<?, ?> raw)) throw new IllegalArgumentException("Expected JSON object");
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private Object parseDocument() {
        Object value = value();
        whitespace();
        if (index != text.length()) fail();
        return value;
    }

    private Object value() {
        whitespace();
        if (index >= text.length()) return fail();
        return switch (text.charAt(index)) {
            case '{' -> object(); case '[' -> array(); case '"' -> string();
            case 't' -> literal("true", true); case 'f' -> literal("false", false);
            case 'n' -> literal("null", null); default -> number();
        };
    }

    private Map<String, Object> object() {
        index++;
        Map<String, Object> map = new LinkedHashMap<>();
        whitespace();
        if (take('}')) return map;
        do {
            whitespace();
            String key = string();
            whitespace();
            if (!take(':')) fail();
            map.put(key, value());
            whitespace();
        } while (take(','));
        if (!take('}')) fail();
        return map;
    }

    private List<Object> array() {
        index++;
        List<Object> list = new ArrayList<>();
        whitespace();
        if (take(']')) return list;
        do { list.add(value()); whitespace(); } while (take(','));
        if (!take(']')) fail();
        return list;
    }

    private String string() {
        if (!take('"')) return fail();
        StringBuilder out = new StringBuilder();
        while (index < text.length()) {
            char c = text.charAt(index++);
            if (c == '"') return out.toString();
            if (c != '\\') { out.append(c); continue; }
            if (index >= text.length()) return fail();
            char escaped = text.charAt(index++);
            if (escaped == 'u') {
                if (index + 4 > text.length()) return fail();
                out.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                index += 4;
            } else {
                out.append(switch (escaped) {
                    case '"' -> '"'; case '\\' -> '\\'; case '/' -> '/'; case 'b' -> '\b';
                    case 'f' -> '\f'; case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t';
                    default -> throw new IllegalArgumentException("Invalid JSON escape");
                });
            }
        }
        return fail();
    }

    private Object number() {
        int start = index;
        while (index < text.length() && "-+0123456789.eE".indexOf(text.charAt(index)) >= 0) index++;
        if (start == index) return fail();
        String token = text.substring(start, index);
        try { return token.contains(".") || token.contains("e") || token.contains("E")
                ? Double.parseDouble(token) : Long.parseLong(token); }
        catch (NumberFormatException badNumber) { return fail(); }
    }

    private Object literal(String token, Object value) {
        if (!text.startsWith(token, index)) return fail();
        index += token.length();
        return value;
    }

    private boolean take(char expected) {
        if (index < text.length() && text.charAt(index) == expected) { index++; return true; }
        return false;
    }

    private void whitespace() { while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++; }
    private <T> T fail() { throw new IllegalArgumentException("Invalid JSON at offset " + index); }
}
