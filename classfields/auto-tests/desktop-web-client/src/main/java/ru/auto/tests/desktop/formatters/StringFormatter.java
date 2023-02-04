package ru.auto.tests.desktop.formatters;

/**
 * User: timondl@yandex-team.ru
 * Date: 19.01.17
 */
public class StringFormatter {

    public static String formatVin(String vin) {
        char[] vinChars = vin.toCharArray();

        vinChars[8] = '*';
        vinChars[11] = '*';
        vinChars[12] = '*';
        vinChars[13] = '*';
        vinChars[14] = '*';

        return new String(vinChars);
    }
}
