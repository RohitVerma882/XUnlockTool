package com.xut.util;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class StringUtils {
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random random = new SecureRandom();

    public static String randomWord(final int length) {
        if (length <= 0) throw new IllegalArgumentException("Length must be greater then 0");
        final var clean = CHARS.length();
        final var builder = new StringBuilder();
        for (var i = 0; i < length; ++i) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                builder.append(CHARS.charAt(random.nextInt(0, clean)));
            } else {
                builder.append(CHARS.charAt(random.nextInt(clean)));
            }
        }
        return builder.toString();
    }

    public static String map2json(final Map<?, ?> map, final int indent) throws JSONException {
        return map2json(map, indent, indent);
    }

    /**
     * @noinspection rawtypes
     */
    private static String map2json(final Map<?, ?> map, final int indent, final int sindent) throws JSONException {
        if (!(map instanceof LinkedHashMap)) {
            return new JSONObject(map).toString(indent);
        }

        final var NL = indent > 0 ? "\n" : "";
        final var IN = indentToString(indent);

        final var builder = new StringBuilder("{");
        for (final Map.Entry entry : map.entrySet()) {
            String toAdd;
            if (entry.getValue() instanceof Number) {
                toAdd = String.valueOf(entry.getValue());
            } else if (entry.getValue() instanceof String) {
                toAdd = '"' + entry.getValue().toString().replace("\"", "\\\"") + '"';
            } else if (entry.getValue() instanceof Map) {
                toAdd = map2json((Map<?, ?>) entry.getValue(), indent + sindent, sindent);
            } else {
                throw new JSONException("Unknown type: " + entry.getValue().getClass().getSimpleName());
            }
            builder.append(NL).append(IN).append('"').append(entry.getKey().toString()).append("\" : ").append(toAdd).append(",");
        }
        return builder.substring(0, builder.length() - 1) + NL + indentToString(indent - sindent) + "}" + (indent == sindent ? NL : "");
    }

    private static String indentToString(final int indent) {
        if (indent <= 0) {
            return "";
        }
        final var ic = new char[indent];
        Arrays.fill(ic, ' ');
        return new String(ic);
    }
}