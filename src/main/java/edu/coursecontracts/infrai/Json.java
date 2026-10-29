package edu.coursecontracts.infrai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.space();
        if (parser.index != source.length()) throw new IllegalArgumentException("Trailing JSON content");
        return value;
    }

    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(quote(entry.getKey().toString())).append(':').append(stringify(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : items) {
                if (!first) out.append(',');
                first = false;
                out.append(stringify(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Cannot encode " + value.getClass().getName());
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('\"').toString();
    }

    private static final class Parser {
        private final String source;
        private int index;
        private Parser(String source) { this.source = source; }
        private void space() { while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++; }
        private char take() { return source.charAt(index++); }
        private Object value() {
            space();
            if (index >= source.length()) throw new IllegalArgumentException("Incomplete JSON");
            return switch (source.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '\"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }
        private Map<String, Object> object() {
            take();
            Map<String, Object> map = new LinkedHashMap<>();
            space();
            if (source.charAt(index) == '}') { index++; return map; }
            while (true) {
                space();
                String key = string();
                space();
                if (take() != ':') throw new IllegalArgumentException("Expected colon");
                map.put(key, value());
                space();
                char separator = take();
                if (separator == '}') return map;
                if (separator != ',') throw new IllegalArgumentException("Expected comma");
            }
        }
        private List<Object> array() {
            take();
            List<Object> list = new ArrayList<>();
            space();
            if (source.charAt(index) == ']') { index++; return list; }
            while (true) {
                list.add(value());
                space();
                char separator = take();
                if (separator == ']') return list;
                if (separator != ',') throw new IllegalArgumentException("Expected comma");
            }
        }
        private String string() {
            if (take() != '\"') throw new IllegalArgumentException("Expected string");
            StringBuilder out = new StringBuilder();
            while (index < source.length()) {
                char c = take();
                if (c == '\"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                char escaped = take();
                switch (escaped) {
                    case '\"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> { out.append((char) Integer.parseInt(source.substring(index, index + 4), 16)); index += 4; }
                    default -> throw new IllegalArgumentException("Bad escape");
                }
            }
            throw new IllegalArgumentException("Unclosed string");
        }
        private Object number() {
            int start = index;
            while (index < source.length() && "-+0123456789.eE".indexOf(source.charAt(index)) >= 0) index++;
            String token = source.substring(start, index);
            return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.valueOf(token) : Long.valueOf(token);
        }
        private Object literal(String token, Object value) {
            if (!source.startsWith(token, index)) throw new IllegalArgumentException("Bad JSON literal");
            index += token.length();
            return value;
        }
    }
}
