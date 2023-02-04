package ru.yandex.arenda.utils;

import com.google.gson.GsonBuilder;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

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

    public static <T> T getObjectFromJson(Class<T> tClass, String path) {
        return new GsonBuilder().create().fromJson(getResourceAsString(path), tClass);
    }

    public static String getHrefForPhone(String phone) {
        return format("tel:%s", phone);
    }

    public static String generateRandomCyrillic(int n) {
        SecureRandom RANDOM = new SecureRandom();
        final String cyrillicCharacters = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ";

        int randomCharIndex;
        StringBuilder numberedString = new StringBuilder();
        for (int i = 0; i < n; i++) {
            randomCharIndex = RANDOM.nextInt(cyrillicCharacters.length());
            numberedString.append(cyrillicCharacters.charAt(randomCharIndex));
        }
        return numberedString.toString();
    }
}
