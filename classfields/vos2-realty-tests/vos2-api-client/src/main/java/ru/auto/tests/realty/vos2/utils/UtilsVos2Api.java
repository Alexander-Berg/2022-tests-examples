package ru.auto.tests.realty.vos2.utils;

import com.google.gson.GsonBuilder;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.awaitility.Duration.ZERO;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

/**
 * Created by vicdev on 23.10.17.
 */
public class UtilsVos2Api {
    private UtilsVos2Api() {
    }

    public static <T> T getRequest(Class<T> tClass) {
        return getObjectFromJson(tClass, "request.json");
    }

    public static <T> T getError(Class<T> tClass) {
        return getObjectFromJson(tClass, "schemas/error_item_resp.json");
    }

    public static <T> T getObjectFromJson(Class<T> tClass, String path) {
        return new GsonBuilder().create().fromJson(getResourceAsString(path), tClass);
    }

    public static int getRandomPrice() {
        return (int) (Math.random() * 9000000 + 1000000);
    }

    public static String reformatOfferCreateDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String getRandomLogin() {
        return "1911671243434496";
    }

    /**
     * Для агентства ОГРН/ОГРНИП 15 любых чисел
     *
     * @return
     */
    public static String getRandomOgrn() {
        return randomNumeric(15);
    }

    public static ConditionFactory apiAwait() {
        return Awaitility.given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .ignoreExceptions()
                .pollDelay(ZERO)
                .pollInterval(3, SECONDS);
    }
}
