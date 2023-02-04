package ru.yandex.realty.utils;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class UtilsWeb {

    public static final String PHONE_PATTERN_BRACKETS = "+%s (%s) %s-%s-%s";
    public static final String PHONE_PATTERN_DASHES = "+%s %s %s-%s-%s";
    public static final String PHONE_PATTERN_SPACES = "+%s %s %s %s %s";

    public static String makePhoneFormatted(String phone, String patternString) {
        Pattern pattern = Pattern.compile("(\\d)(\\d{3})(\\d{3})(\\d{2})(\\d{2})");
        Matcher matcher = pattern.matcher(phone);
        matcher.find();
        String[] groups = new String[5];
        for (int i = 0; i < matcher.groupCount(); i++) {
            groups[i] = matcher.group(i + 1);
        }
        return format(patternString, groups);
    }

    public static String getHrefForPhone(String phone) {
        return format("tel:%s", phone);
    }

    //выдаем цену от 6 до 12 млн помиллионно
    public static int getNormalPrice() {
        return ((new Random()).nextInt(6) + 7) * 1000000;
    }

    //выдаем площадь от 40 до 80 кв м
    public static int getNormalArea() {
        return (40 + (new Random()).nextInt(41));
    }
}
