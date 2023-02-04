package ru.auto.tests.desktop.step;

import io.qameta.allure.Step;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.CoreMatchers;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.api.UserApi;
import ru.auto.tests.publicapi.api.UserPhonesApi;
import ru.auto.tests.publicapi.model.AutoApiAddIdentityResponse;
import ru.auto.tests.publicapi.model.AutoApiOffersSaveSuccessResponse;
import ru.auto.tests.publicapi.model.VertisPassportAddPhoneParameters;
import ru.auto.tests.publicapi.model.VertisPassportConfirmIdentityParameters;
import ru.auto.tests.publicapi.model.VertisPassportConfirmIdentityResult;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

public class PublicApiSteps {

    private String sessionId;
    private String phone;

    @Inject
    private PublicApiAdaptor publicApiAdaptor;

    @Inject
    private PassportApiAdaptor passportApiAdaptor;

    @Inject
    @Prod
    private ApiClient api;

    @Inject
    public CookieSteps cookieSteps;

    @Step("Добавляем текущему аккаунту телефон «{phone}»")
    public AutoApiAddIdentityResponse addPhone(String phone) {
        UserPhonesApi.AddPhoneOper addPhoneOper = this.api.userPhones().addPhone().reqSpec(defaultSpec())
                .body((new VertisPassportAddPhoneParameters()).phone(phone).confirmed(false))
                .xSessionIdHeader(sessionId);

        return Awaitility.given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(Duration.ZERO).pollInterval(3L, TimeUnit.SECONDS)
                .atMost(60L, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
                    return addPhoneOper.executeAs(validatedWith(shouldBe200OkJSON()));
                }, CoreMatchers.notNullValue());
    }

    @Step("Подтверждаем телефон «{phone}»")
    public VertisPassportConfirmIdentityResult confirmPhone(String phone) {
        String code = passportApiAdaptor.getLastSmsCode(phone);
        UserApi.ConfirmIdentityOper confirmIdentityOper = this.api.user().confirmIdentity().reqSpec(defaultSpec())
                .body((new VertisPassportConfirmIdentityParameters()).phone(phone).code(code));

        return Awaitility.given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(Duration.ZERO).pollInterval(3L, TimeUnit.SECONDS)
                .atMost(60L, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
                    return confirmIdentityOper.executeAs(validatedWith(shouldBe200OkJSON()));
                }, CoreMatchers.notNullValue());
    }

    @Step("Создаем оффер ждём его активации")
    public String createOffer() {
        AutoApiOffersSaveSuccessResponse createOfferResponse = publicApiAdaptor.createOffer(
                phone,
                sessionId,
                CARS);

        String offerId = createOfferResponse.getOfferId();

        publicApiAdaptor.waitOfferActivation(
                sessionId,
                CARS,
                offerId);

        return offerId;
    }

    @Step("Удаляем оффер «{offerId}»")
    public void deleteOffer(String offerId) {
        publicApiAdaptor.deleteOffer(
                sessionId,
                CARS,
                offerId);
    }

    public void addAndConfirmPhone() {
        sessionId = cookieSteps.getSessionId();
        phone = Utils.getRandomPhone().replaceFirst("^7000", "7985");

        addPhone(phone);
        confirmPhone(phone);
    }

}
