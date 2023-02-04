package ru.auto.tests.passport.core;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.passport.client.CaptchaApiService;
import ru.auto.tests.passport.client.PassportApiClient;
import ru.auto.tests.passport.client.PassportApiService;
import ru.auto.tests.passport.config.PassportConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static ru.auto.tests.passport.client.Constants.COMMON_PASSWD1234567;
import static ru.auto.tests.passport.client.Constants.DEFAULT_FIRSTNAME;
import static ru.auto.tests.passport.client.Constants.DEFAULT_LASTNAME;
import static ru.auto.tests.passport.client.Constants.STR_RU;
import static ru.auto.tests.passport.client.Constants.STR_TRUE;
import static ru.auto.tests.passport.client.Constants.USER_HOST;
import static ru.auto.tests.passport.client.Constants.USER_YA_IP;
import static ru.auto.tests.passport.client.Constants.VALIDATION_METHOD;
import static ru.auto.tests.passport.core.PassportUtils.checkPhoneNumberIsValid;
import static ru.auto.tests.passport.core.PassportUtils.disableProxyIf;
import static ru.auto.tests.passport.core.PassportUtils.enableProxyIf;
import static ru.auto.tests.passport.core.PassportUtils.getRandomInternalUserIp;
import static ru.auto.tests.passport.core.PassportUtils.getRandomTestReqId;
import static ru.auto.tests.passport.core.PassportUtils.getRandomUserIp;
import static ru.auto.tests.passport.core.PassportUtils.ok200JsonResponse;

/**
 * Created by vicdev on 26.04.17.
 */
public class PassportAdaptor extends AbstractModule {

    @Inject
    private PassportConfig config;

    @Inject
    private PassportApiClient yandexPassport;

    @Step("Регистрируем телефон {phone} для пользователя {accountId}")
    public void addPhone(String accountId, String phone) {
        checkPhoneNumberIsValid(phone);
        try {
            enableProxyIf(config.isLocalDebug(), config.proxyHost(), config.proxyPort());
            String consumer = config.consumer();
            PassportApiService service = yandexPassport.createService(PassportApiService.class);
            String trackId = ok200JsonResponse(service.confirmAndBindSecureSubmit(getRandomUserIp(), getRandomTestReqId(),
                    consumer, STR_RU, accountId, phone)).body().getTrackId();
            String code = ok200JsonResponse(service.rereadTrack(USER_YA_IP, getRandomTestReqId(), consumer, trackId)).body()
                    .getPhoneConfirmationCode();
            ok200JsonResponse(service.confirmAndBindSecureCommit(getRandomUserIp(), getRandomTestReqId(), consumer,
                    trackId, code, COMMON_PASSWD1234567));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            disableProxyIf(config.isLocalDebug());
        }
    }

    @Step("Получаем код подверждения телефона для пользователя текущего пользователя")
    public String getConfirmationCode(String trackId) {
        enableProxyIf(config.isLocalDebug(), config.proxyHost(), config.proxyPort());
        String consumer = config.consumer();
        PassportApiService service = yandexPassport.createService(PassportApiService.class);
        String code = given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions()
                .until(() -> ok200JsonResponse(service.rereadTrack(USER_YA_IP, getRandomTestReqId(), consumer, trackId))
                        .body().getPhoneConfirmationCode(), notNullValue());
        disableProxyIf(config.isLocalDebug());
        return code;
    }

    @Step("Получаем ответ для капчи с ключом: {key}")
    public String getCaptchaAnswer(String key) {
        enableProxyIf(config.isLocalDebug(), config.proxyHost(), config.proxyPort());
        CaptchaApiService captcha = yandexPassport
                .setRetrofitBuilder(new Retrofit
                        .Builder().baseUrl(config.getApiCapthaUrl())
                        .addConverterFactory(SimpleXmlConverterFactory.create()))
                .createService(CaptchaApiService.class);
        String code = given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .ignoreExceptions().await()
                .until(() -> captcha.captcha(getRandomTestReqId(), key).execute().body().getAnswer(),
                        not(isEmptyOrNullString()));
        disableProxyIf(config.isLocalDebug());
        return code;
    }

    @Step("Создаем аккаунт {login}, с телефоном {phone}")
    public String createPassportAccountWithPhone(String phone, String login) {
        try {
            String userIp = getRandomUserIp();
            String consumer = config.consumer();
            PassportApiService service = yandexPassport.createService(PassportApiService.class);
            String trackId = ok200JsonResponse(service.track(userIp, getRandomTestReqId(), consumer)).body().getId();
            ok200JsonResponse(service
                    .sendConfirmationCode(userIp, getRandomTestReqId(), consumer, "ru", phone, trackId));
            String confirmationCode = ok200JsonResponse(service.rereadTrack(USER_YA_IP, getRandomTestReqId(),
                    consumer, trackId)).body().getPhoneConfirmationCode();
            ok200JsonResponse(service.completeConfirmation(userIp, getRandomTestReqId(), consumer, confirmationCode,
                    trackId));
            return given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                    .ignoreExceptions().await().pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> ok200JsonResponse(service.registerWithPhone(getRandomInternalUserIp(), getRandomTestReqId(),
                    consumer, VALIDATION_METHOD, login, COMMON_PASSWD1234567, trackId, DEFAULT_FIRSTNAME,
                            DEFAULT_LASTNAME, STR_RU, STR_RU, STR_TRUE)).body().getUid(), notNullValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Создаем аккаунт {login} без телефона")
    public String createPassportAccount(String login) {
        try {
            String consumer = config.consumer();
            PassportApiService service = yandexPassport.createService(PassportApiService.class);
            return ok200JsonResponse(service.registerWithoutPhone(getRandomInternalUserIp(), getRandomTestReqId(),
                    consumer, login, COMMON_PASSWD1234567, DEFAULT_FIRSTNAME, DEFAULT_LASTNAME, STR_RU, STR_RU)).body()
                    .getUid();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Удаляем аккаунт: {uid}")
    public void deleteAccount(String uid) {
        try {
            PassportApiService service = yandexPassport.createService(PassportApiService.class);
            ok200JsonResponse(service.delete(uid, getRandomUserIp(), USER_HOST, getRandomTestReqId(),
                    config.consumer()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void configure() {
    }
}
