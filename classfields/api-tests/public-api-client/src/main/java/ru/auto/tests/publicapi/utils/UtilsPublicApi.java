package ru.auto.tests.publicapi.utils;

import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.model.AutoApiDevice.PlatformEnum;
import ru.auto.tests.publicapi.model.AutoApiHelloRequest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static ru.auto.tests.publicapi.model.AutoApiDevice.PlatformEnum.IOS;

/**
 * Created by vicdev on 23.10.17.
 */
public class UtilsPublicApi {
    private UtilsPublicApi() {
    }

    public static String getRandomDeviceId() {
        return String.format("%s.%s", Utils.getRandomString(32), Utils.getRandomString(32));
    }

    public static String getRandomOfferId() {
        return String.format("%s-%s", Utils.getRandomShortInt(), Utils.getRandomString(6));
    }

    public static String getRandomDraftId() {
        return String.format("%s-%s", Utils.getRandomShortInt(), Utils.getRandomString(10).replaceAll("[G-Zg-z]*","F"));
    }

    public static String getRandomTime() {
        Random rnd = new Random();
        Date date = new Date(Math.abs(System.currentTimeMillis() - rnd.nextLong()));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:00");
        return sdf.format(date);
    }

    public static AutoApiHelloRequest getRandomHelloRequest(PlatformEnum platform) {
        AutoApiHelloRequest hr = random(AutoApiHelloRequest.class, "supportedFeatures");
        hr.getDevice().setPlatform(platform);

        if (platform != IOS) {
            hr.getDevice().setIosDeviceCheckToken(null);
        }

        return hr;
    }

    public static String getCurrentTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Long date = System.currentTimeMillis();
        return dateFormat.format(date);
    }

    public static String getTimeDaysAgo(int days) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Long date = System.currentTimeMillis() - java.time.Duration.ofDays(days).toMillis();
        return dateFormat.format(date);
    }

    public static String getTimeDaysForward(int days) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Long date = System.currentTimeMillis() + java.time.Duration.ofDays(days).toMillis();
        return dateFormat.format(date);
    }

    public static String getRandomVin() {
        return String.format("3N1CN7AP9FL%s", randomNumeric(6));

    }

    public static String getRandomLicensePlate() {
        return String.format("A%sAA164", randomNumeric(3));
    }

    public static String getRandomCreditSecretWord() {
        return String.format("СЕКРЕТ%s", randomNumeric(5));
    }

    public static String getRandomArticleId() {
        return String.format("%s-%s", Utils.getRandomString(6), Utils.getRandomString(6));
    }
}
