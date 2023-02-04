package ru.auto.tests.realtyapi.utils;

import com.google.common.base.Splitter;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realtyapi.v1.model.QueryExample;
import ru.auto.tests.realtyapi.v1.model.SubscriptionRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

import static java.nio.file.Files.readAllLines;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.awaitility.Duration.ZERO;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum;

/**
 * Created by vicdev on 23.10.17.
 */
public class UtilsRealtyApi {
    private UtilsRealtyApi() {
    }

    public static String getAutoTestsPhone() {
        return "+79998883333";
    }

    public static String getRandomOfferId() {
        return "7116712453223434496";
    }

    public static String getRandomUID() {
        return "1111111111";
    }

    public static double getRandomPrice() {
        return 9223131.50;
    }

    public static int getRandomIntForSubscription() {
        return (new Random()).nextInt(100) + 1;
    }

    public static String getRandomStringForSubscription() {
        return randomAlphabetic(15);
    }

    public static String getEmptyBody() {
        return "{}";
    }

    public static String getDefaultTestName() {
        return "Def-Имя-autotests";
    }

    public static SubscriptionRequest getValidSubscriptionRequest() {
        Long[] validRgids = new Long[]{
                587795L, // Moscow
                417899L, //  Saint P.
                417941L, // Kronshtadt
                353118L, // Krasnodarskiy krai
                573359L, // Vologda
                2318L, // okrug Khimki
                593545L, // Khimki
                406766L, // Kaliningrad
                585430L, // Vladivostok
                16898142L, // Ulan-Ude
                552970L // Omsk
        };

        // Вычисляем рандомный индекс
        int offerCategoryIndex = getRandomIntForSubscription() % OfferCategoryEnum.values().length;
        int offerTypeIndex = getRandomIntForSubscription() % OfferTypeEnum.values().length;
        int rgidIndex = getRandomIntForSubscription() % validRgids.length;

        String offerCategory = OfferCategoryEnum.values()[offerCategoryIndex].getValue();
        String offerType = OfferTypeEnum.values()[offerTypeIndex].getValue();
        QueryExample reqQuery = new QueryExample()
                .type(offerType)
                .category(offerCategory)
                .rgid(validRgids[rgidIndex]);

        return new SubscriptionRequest()
                .query(reqQuery)
                .title(getRandomStringForSubscription())
                .token(getRandomStringForSubscription())
                .period(getRandomIntForSubscription());
    }

    public static String getYesterdayDate() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE, -1);
        return date.getTime().toInstant().toString();
    }

    public static Collection<Object[]> getQueryMapParams(String resource) throws IOException {
        InputStream dataStream = UtilsRealtyApi.class.getClassLoader().getResourceAsStream(resource);
        return (Collection<Object[]>) IOUtils.readLines(dataStream, UTF_8).stream()
                .map(params -> new Object[]{Splitter.on("&").withKeyValueSeparator("=").split(params.toString())})
                .collect(Collectors.toList());
    }

    public static JsonArray getJsonArrayFromString(String jsonString) {
        return new GsonBuilder().setPrettyPrinting().create()
                .fromJson(jsonString, JsonElement.class)
                .getAsJsonArray();
    }

    public static Collection<Object[]> parseParams(String testName, String resource) throws IOException {
        Path path = Paths.get("src/test/resources/" + resource);
        return readAllLines(path).stream().map(string -> string.split(" "))
                .filter(array -> array[0].equals(testName))
                .map(array -> new Object[]{Splitter.on("&")
                        .withKeyValueSeparator("=")
                        .split(array[1])})
                .collect(Collectors.toList());
    }

    public static String getUid(Account account) {
        return "uid:" + account.getId();
    }

//    public static RealtyApiRestoreBatchRequest getRestoreFilterWithOfferId(String offerId) {
//        return new RealtyApiRestoreBatchRequest()
//                .filter(getOfferIdFilter(offerId));
//    }
//
//    public static RealtyApiDeleteBatchRequest getDeleteFilterWithOfferId(String offerId) {
//        return new RealtyApiDeleteBatchRequest()
//                .filter(getOfferIdFilter(offerId));
//    }
//
//    public static RealtyApiUpdateStatusBatchRequest getUpdateFilterWithOfferId(String offerId) {
//        return new RealtyApiUpdateStatusBatchRequest()
//                .filter(getOfferIdFilter(offerId));
//    }
//
//    public static RealtyApiOfferFilter getOfferIdFilter(String offerId) {
//        return new RealtyApiOfferFilter().addOfferIdsItem(offerId);
//    }

    public static ConditionFactory apiAwait() {
        return Awaitility.given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .ignoreExceptions()
                .pollDelay(ZERO)
                .pollInterval(3, SECONDS);
    }
}
