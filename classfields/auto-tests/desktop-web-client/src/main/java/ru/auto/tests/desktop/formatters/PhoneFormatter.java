package ru.auto.tests.desktop.formatters;

/**
 * User: timondl@yandex-team.ru
 * Date: 25.09.16
 */
public class PhoneFormatter {

    /**
     * Convert Long phone number to printable format
     *
     * @param format      Like '+%s %s %s-%s-%s'
     * @param phoneNumber Phone, needed to format
     * @return Formatted phone
     */
    public static String simpleFormat(String format, String phoneNumber) {
        return String.format(
                format,
                phoneNumber.substring(0, 1),
                phoneNumber.substring(1, 4),
                phoneNumber.substring(4, 7),
                phoneNumber.substring(7, 9),
                phoneNumber.substring(9, 11)
        );
    }
}
