package ru.auto.tests.commons.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.kohsuke.randname.RandomNameGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

/**
 * Created by vicdev on 08.02.17.
 */
public class Utils {

    private static final RandomNameGenerator RANDOM_NAME_GENERATOR = new RandomNameGenerator();

    private Utils() {
    }

    /**
     * @return рандомная человекочитаемая строка, например 'vicious_structure'
     */
    public static String getRandomString() {
        return RANDOM_NAME_GENERATOR.next();
    }

    public static String getRandomString(int count) {
        return RandomStringUtils.randomAlphabetic(count);
    }

    public static int getRandomShortInt() {
        return (new Random()).nextInt(6) + 1;
    }

    public static long getRandomShortLong() {
        return (long) getRandomShortInt();
    }

    public static boolean getRandomBoolean() {
        return (new Random()).nextBoolean();
    }

    /**
     * @return рандомный НЕсуществующий почтовый ящик в домене @blah
     * Например:
     * linear_fund@blah.ru
     */
    public static String getRandomEmail() {
        return String.format("%s@blah.ru", Utils.getRandomString());
    }

    /**
     * @return рандомный НЕсуществующий номер, начинающийся с 7000
     * Например:
     * 70001234567
     */
    public static String getRandomPhone() {
        return "7000" + randomNumeric(7);
    }

    public static String getResourceAsString(String path) {
        String result;
        try {
            result = IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(path),
                    StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException("can't read path " + path, e);
        }

        return result;
    }
}