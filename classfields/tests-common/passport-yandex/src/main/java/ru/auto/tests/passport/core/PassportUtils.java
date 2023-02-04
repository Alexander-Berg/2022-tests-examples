package ru.auto.tests.passport.core;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.StepResult;
import org.apache.commons.lang3.RandomUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.qameta.allure.model.Status.PASSED;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by vicdev on 26.04.17.
 */
public class PassportUtils {

    private PassportUtils() {
    }

    static void enableProxy(String host, String port) {
        System.setProperty("proxySet", "true");
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port);
    }

    static void setBaseScheme(String type) {
        System.setProperty("base.scheme", type);
    }

    static void setConsumer(String consumer) {
        System.setProperty("passport.consumer", consumer);
    }

    static void disableProxy() {
        System.setProperty("proxySet", "false");
        System.setProperty("http.proxyHost", "");
        System.setProperty("http.proxyPort", "");
    }

    static void enableProxyIf(boolean cond, String host, String port) {
        if (cond) {
            enableProxy(host, port);
        }
    }

    static void disableProxyIf(boolean cond) {
        if (cond) {
            disableProxy();
        }
    }

    public static String getRandomUserIp() {
        return String.format("127.%d.%d.%d", nextInt(1, 256), nextInt(1, 256), nextInt(1, 256));
    }

    public static String getRandomInternalUserIp() {
        return "37.9.101." + RandomUtils.nextInt(1, 256);
    }

    public static String getRandomYandexTeamLogin() {
        return new Random().nextBoolean() ? "yandex-team-" + randomNumeric(9) : "yndx-" + randomNumeric(9);
    }

    public static String getRandomYndxCaptchaNever() {
        return "yndx-captcha-never-" + randomNumeric(9);
    }

    public static String getRandomTestReqId() {
        return randomAlphanumeric(10);
    }

    public static String getRandomFakePhoneInE164() {
        return "+70000" + randomNumeric(6);
    }

    public static void checkPhoneNumberIsValid(String phone) {
        Pattern pattern = Pattern.compile("[+7]{1}[0-9]{2,12}");
        Matcher matcher = pattern.matcher(phone);
        if (!matcher.matches() || phone.length() < 12) {
            throw new IllegalArgumentException("Phone invalid. Should be like +7[8120223344]");
        }
    }

    public static <T> Response<T> ok200JsonResponse(Call<T> call) throws IOException {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        lifecycle.startStep(
                UUID.randomUUID().toString(),
                new StepResult().setStatus(PASSED).setName(String.format("%s: %s", call.request().method(), call.request().url().toString()))
        );
        Response<T> response;
        try {
            response = call.execute();
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.headers().get("Content-Type")).containsIgnoringCase("json");
            assertThat(response.message()).isEqualToIgnoringCase("ok");
        } finally {
            lifecycle.stopStep();
        }
        return response;
    }
}
