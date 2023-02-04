package ru.yandex.realty.adaptor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.hamcrest.Matcher;
import ru.auto.test.api.realty.ApiVos2;
import ru.auto.test.api.realty.OfferType;
import ru.auto.test.api.realty.draft.create.userid.CreateDraftReq;
import ru.auto.test.api.realty.offer.create.userid.CreateOfferReq;
import ru.auto.test.api.realty.offer.create.userid.Offer;
import ru.auto.test.api.realty.offer.create.userid.Request;
import ru.auto.test.api.realty.offer.create.userid.responses.CreateOfferResp;
import ru.auto.test.api.realty.offer.delete.userid.offerid.DeleteOfferReq;
import ru.auto.test.api.realty.offer.updateprice.userid.offerid.Price;
import ru.auto.test.api.realty.offer.updateprice.userid.offerid.UpdatePrice;
import ru.auto.test.api.realty.offer.updateprice.userid.offerid.responses.UpdatePriceResp;
import ru.auto.test.api.realty.offer.userid.offerid.responses.OfferInfo;
import ru.auto.test.api.realty.user.create.UserReq;
import ru.auto.test.api.realty.user.userid.responses.GetUserResp;
import ru.auto.test.api.realty.useroffers.userid.responses.UserOffersResp;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;

import java.util.List;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.test.api.realty.VosOfferStatus.ACTIVE;
import static ru.auto.test.api.realty.VosOfferStatus.BANNED;
import static ru.auto.test.api.realty.VosOfferStatus.TRUSTED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasStatus;
import static ru.yandex.realty.matchers.OfferMatcher.offerShould;
import static ru.yandex.realty.matchers.TransactionStatusMatcher.transactionIsActive;
import static ru.yandex.realty.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.realty.response.ResponseSpecifications.shouldBeStatusOk;
import static ru.yandex.realty.utils.RealtyUtils.getObjectFromJson;
import static ru.yandex.realty.utils.RealtyUtils.getRequest;

/**
 * Created by vicdev on 10.04.17.
 */
public class Vos2Adaptor extends AbstractModule {

    public static final String SERVICE_NAME = "realty";

    @Inject
    private ApiVos2 vos2;

    @Step("Получаем массив объявлений у пользователя {uid}")
    public UserOffersResp waitUserOffers(String uid) {
        waitOffers(uid);
        return getUserOffers(uid);
    }

    @Step("Получаем массив объявлений у пользователя {uid}")
    public UserOffersResp getUserOffers(String uid) {
        return vos2.userOffers().userID().withUserID(uid).withDefaults()
                .get(validatedWith(shouldBeStatusOk())).as(UserOffersResp.class, GSON);
    }

    @Step("Получаем объявление по uid={userId} и id={offerId} офера")
    public OfferInfo getOffer(String userId, String offerId) {
        return vos2.offer().userID().withUserID(userId).offerID().withOfferID(offerId)
                .get(validatedWith(shouldBeStatusOk()))
                .as(OfferInfo.class, GSON);
    }

    @Step("Создаем рандомный аккаунт в VOS2 у пользователя ({req})")
    public void createVos2Account(UserReq req) {
        int statusCode = vos2.user().create().withUserReq(req).post(identity()).getStatusCode();
        assertThat(statusCode, anyOf(equalTo(SC_OK), equalTo(SC_CONFLICT)));
    }

    @Step("Создаем объявление у пользователя ({uid}) в VOS2")
    public CreateOfferResp createOffers(String uid, List<Offer> offers) {
        return vos2.offer().create().userID().withUserID(uid)
                .withCreateOfferReq(new CreateOfferReq().withRequest(
                        getRequest(Request.class))
                        .withOffers(offers)).post(validatedWith(shouldBe200OkJSON()))
                .as(CreateOfferResp.class, GSON);
    }

    @Step("Создаем черновик у пользователя ({uid}) в VOS2")
    public CreateDraftReq createDraft(String uid, List<ru.auto.test.api.realty.draft.create.userid.Offer> offers) {
        return vos2.draft().create().userID().withUserID(uid)
                .withCreateDraftReq(new CreateDraftReq().withRequest(
                        getRequest(ru.auto.test.api.realty.draft.create.userid.Request.class))
                        .withOffers(offers)).post(validatedWith(shouldBe200OkJSON()))
                .as(CreateDraftReq.class, GSON);
    }

    @Step("Удаляем объявление с id {offerId} у пользователя {uid}")
    public void deleteOffer(String uid, String offerId) {
        vos2.offer().delete().userID().offerID().withUserID(uid).withOfferID(offerId)
                .withDeleteOfferReq(new DeleteOfferReq().withRequest(
                        getRequest(ru.auto.test.api.realty.offer.delete.userid.offerid.Request.class)))
                .delete(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Ждем аккаунт с uid {uid}")
    private void waitAccount(String uid) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .ignoreExceptions().await().until(() -> vos2.user().userID().withUserID(uid).get(identity()
        ).statusCode(), equalTo(SC_OK));
    }

    @Step("Ждем объявления у пользователя с uid {uid}")
    private void waitOffers(String uid) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .ignoreExceptions().alias("Ждем объявления у пользователя")
                .pollInterval(3, SECONDS).atMost(30, SECONDS).until(
                () -> vos2.userOffers().userID().withUserID(uid).withDefaults()
                        .get(validatedWith(shouldBeStatusOk()))
                        .as(UserOffersResp.class, GSON).getPager().getTotalItems(), greaterThanOrEqualTo((long) 1));
    }

    @Step("Получаем информацию об аккаунте ({uid}) в vos2")
    public GetUserResp getVos2Account(String uid) {
        waitAccount(uid);
        return vos2.user().userID().withUserID(uid).get(validatedWith(shouldBeStatusOk()))
                .as(GetUserResp.class, GSON);
    }

    @Step("Ждём активации офера «{offerId}»")
    public void waitActivateOffer(String uid, String offerId) {
        assertThat("Оффер не был активирован", vos2,
                withWaitFor(offerShould(anyOf(hasStatus(ACTIVE.toString()),
                        hasStatus(TRUSTED.toString()))).setUserId(uid).setOfferId(offerId),
                        SECONDS.toMillis(51), SECONDS.toMillis(2)));
    }

    @Step("Ждём, что статус оффера «{offerId}» станет «banned»")
    public void waitBanOffer(String uid, String offerId) {
        assertThat("Оффер не ушел с модерации", vos2,
                withWaitFor(offerShould(hasStatus(BANNED.toString())).setUserId(uid).setOfferId(offerId),
                        SECONDS.toMillis(51), SECONDS.toMillis(2)));
    }

    @Step("Ждём оффер «{offerInfoMatcher}»")
    public void waitOffer(String uid, String offerId, Matcher<OfferInfo> offerInfoMatcher) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await()
                .pollInterval(3, SECONDS).atMost(45, SECONDS)
                .until(() -> vos2.offer().userID().withUserID(uid).offerID().withOfferID(offerId)
                                .get(validatedWith(shouldBeCode(SC_OK)))
                                .as(OfferInfo.class, GSON),
                        offerInfoMatcher);
    }

    @Step("Ждём активации транзакции")
    public void waitActivateTransaction(String uid, String offerId) {
        assertThat("Транзакция не была активирована", vos2, withWaitFor(transactionIsActive(uid, offerId),
                SECONDS.toMillis(51), SECONDS.toMillis(2)));
    }

    @Step("Проверяем, есть ли пользователь ({uid}) в VOS")
    public boolean isVosUser(String uid) {
        return vos2.user().userID().withUserID(uid).get(identity()).statusCode() == SC_OK;
    }


    @Step("Обновляем цену оффера {offerId} у пользователя {uid}")
    public void updatePrice(String uid, String offerId, Long price, OfferType offerType) {
        vos2.offer().updatePrice().userID().withUserID(uid).offerID().withOfferID(offerId)
                .withUpdatePrice(new UpdatePrice()
                        .withRequest(getRequest(ru.auto.test.api.realty.offer.updateprice.userid.offerid.Request.class)
                                .withYandexuid(uid))
                        .withPrice(getObjectFromJson(Price.class, format("prices/%s_price.json", offerType.value()))
                                .withValue(price)))
                .put(validatedWith(shouldBeCode(SC_OK)))
                .as(UpdatePriceResp.class, GSON);
    }

    @Override
    protected void configure() {
    }
}
