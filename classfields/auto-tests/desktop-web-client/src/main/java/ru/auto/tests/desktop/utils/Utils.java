package ru.auto.tests.desktop.utils;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.kohsuke.randname.RandomNameGenerator;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
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

    public static int getRandomBetween(int begin, int end) {

        return (new Random()).nextInt(end - begin) + begin;
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
        return String.format("%s@blah.ru", ru.auto.tests.commons.util.Utils.getRandomString());
    }

    /**
     * @return рандомный НЕсуществующий номер, начинающийся с 7000
     * Например:
     * 70001234567
     */
    public static String getRandomPhone() {
        return "000" + randomNumeric(7);
    }

    public static String getRandomVin() {
        return "A" + randomNumeric(16);
    }

    public static String getRandomId() {
        return String.valueOf(getRandomBetween(1000000000, 2000000000));
    }

    public static String getRandomOfferId() {
        return format("%s-%d",
                getRandomId(),
                getRandomBetween(10000000, 90000000));
    }

    public static String getResourceAsString(String path) {
        String result;
        try {
            result = IOUtils.toString(ru.auto.tests.commons.util.Utils.class.getClassLoader().getResourceAsStream(path),
                    Charsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException("can't read path " + path);
        }

        return result;
    }

    public static String getMatchedString(String str, String regexp) {
        Matcher m = Pattern.compile(regexp).matcher(str);
        m.find();
        return m.group(1);
    }

    public static BufferedImage getResourceAsImage(String path) throws IOException {
        return ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream(path));
    }

    @Step("Парсер: String => Double  ({0})")
    public static Double getStringAsDouble(String string) {
        return Double.parseDouble(string.replaceAll("[^\\d.]", ""));
    }

    @Step("Парсер: String => int ({0})")
    public static int getStringAsInt(String string) {
        return Integer.parseInt(string.replaceAll("\\D+", ""));
    }

    @Step("Парсер: Int => String ({0})")
    public static String getIntAsString(Integer i) {
        return Integer.toString(i);
    }

    public static ArrayListMultimap<String, String> splitQuery(String query) {
        ArrayListMultimap<String, String> queryPairs = ArrayListMultimap.create();
        if (query == null) return queryPairs;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx < 0) return queryPairs;
            queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return queryPairs;
    }

    @Step("Приводим марку или модель, полученные из серчера, к формату фронта")
    public static String modifyMarkModel(String string) {
        return string.replaceAll("\\s+", "_").replaceAll("-", "_");
    }

    @Step("Получаем текущий год")
    public static int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static String pathByOfferCategory(AutoApiOffer.CategoryEnum category) {
        switch (category) {
            case CARS:
                return "cars";
            case MOTO:
                return "moto";
            case TRUCKS:
                return "commercial";
            default:
                throw new IllegalArgumentException("Unknown category");
        }
    }

    public static String expectedPathByCategory(AutoApiOffer.CategoryEnum category) {
        switch (category) {
            case CARS:
                return "cars";
            case MOTO:
                return "motorcycle";
            case TRUCKS:
                return "truck";
            default:
                throw new IllegalArgumentException("Unknown category");
        }
    }

    public static JsonObject getJsonObject(Object obj) {
        return new Gson().toJsonTree(obj).getAsJsonObject();
    }

    public static JsonArray getJsonArray(Object obj) {
        return new Gson().toJsonTree(obj).getAsJsonArray();
    }

    public static JsonObject getJsonByPath(String path) {
        JsonObject jsonObject;

        try {
            jsonObject = new Gson().fromJson(getResourceAsString(path), JsonObject.class);
        } catch (NullPointerException e) {
            throw new RuntimeException(format("Can't read file '%s'", path), e.getCause());
        }

        return jsonObject;
    }

    public static String formatNumber(int number, char separator) {
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);

        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setGroupingSeparator(separator);

        decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);

        return decimalFormat.format(number);
    }

    public static String formatNumber(int number) {
        return formatNumber(number, ' ');
    }

    public static String formatPrice(int price, char separator) {
        return format("%s%s₽", formatNumber(price, separator), separator);
    }

    public static String formatPrice(int price) {
        return formatPrice(price, ' ');
    }

    public static String getISO8601Date(int daysCountSinceToday) {
        return LocalDateTime.now(Clock.systemUTC()).plusDays(daysCountSinceToday).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }

    public static String getRuDateByPattern(int daysSinceToday, String pattern) {
        return LocalDate.parse(LocalDate.now().toString()).plusDays(daysSinceToday).format(
                DateTimeFormatter.ofPattern(pattern).withLocale(new Locale("ru")));
    }

}
