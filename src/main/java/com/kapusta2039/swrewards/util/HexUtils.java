package com.kapusta2039.swrewards.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработка hex-цветов от Overwrite (Спасибо ❤️)
 * https://spigotmc.ru/resources/oshibki-nachinajuschix-razrabotchikov-majnkraft-plaginov-i-kak-ix-ne-dopuskat.2997/
 */
public final class HexUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");
    public static final char COLOR_CHAR = '§';

    private HexUtils() {
    }

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder(message.length() + 32);

        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(builder,
                    COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0)
                    + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2)
                    + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4)
                    + COLOR_CHAR + group.charAt(5));
        }
        message = matcher.appendTail(builder).toString();
        return translateAlternateColorCodes('&', message);
    }

    private static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for (int i = 0, length = b.length - 1; i < length; ++i) {
            if (b[i] == altColorChar && isValidColorCharacter(b[i + 1])) {
                b[i++] = COLOR_CHAR;
                b[i] |= 0x20;
            }
        }

        return new String(b);
    }

    private static boolean isValidColorCharacter(char c) {
        return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                 'a', 'b', 'c', 'd', 'e', 'f',
                 'A', 'B', 'C', 'D', 'E', 'F',
                 'r', 'R', 'k', 'K', 'l', 'L', 'm', 'M',
                 'n', 'N', 'o', 'O', 'x', 'X' -> true;
            default -> false;
        };
    }
}
