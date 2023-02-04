package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.assertj.core.api.Java6Assertions;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import ru.auto.test.api.realty.ApiRealtyBack;
import ru.auto.test.api.realty.ApiSearcher;
import ru.auto.test.api.realty.ApiVos2;
import ru.auto.test.api.realty.OfferType;
import ru.auto.test.api.realty.SubscriptionStatus;
import ru.auto.test.api.realty.card.json.responses.Card;
import ru.auto.test.api.realty.offer.create.userid.Offer;
import ru.auto.test.api.realty.offer.userid.offerid.responses.OfferAssert;
import ru.auto.test.api.realty.offer.userid.offerid.responses.OfferInfo;
import ru.auto.test.api.realty.promocode.CreatePromoBody;
import ru.auto.test.api.realty.service.user.user.subscriptions.responses.EmailAssert;
import ru.auto.test.api.realty.service.user.user.subscriptions.responses.SubscriptionsResp;
import ru.auto.test.api.realty.service.user.user.subscriptions.responses.SubscriptionsRespAssert;
import ru.auto.test.api.realty.user.create.UserReq;
import ru.auto.test.api.realty.user.userid.responses.GetUserResp;
import ru.auto.test.api.realty.user.userid.responses.UserAssert;
import ru.auto.test.api.realty.useroffers.userid.responses.UserOffersResp;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.core.PassportAdaptor;
import ru.yandex.realty.adaptor.BackRtAdaptor;
import ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier;
import ru.yandex.realty.adaptor.PromocodeAdaptor;
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.beans.juridical.JuridicalUserBody;
import ru.yandex.realty.beans.juridical.Phone;
import ru.yandex.realty.beans.juridical.UserContacts;
import ru.yandex.realty.utils.AccountType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static ru.auto.test.api.realty.VosOfferStatus.INACTIVE;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.defaultPromo;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoConstrains;
import static ru.yandex.realty.adaptor.PromocodeAdaptor.promoFeature;
import static ru.yandex.realty.response.ResponseSpecifications.shouldBeStatusOk;
import static ru.yandex.realty.utils.RealtyUtils.getObjectFromJson;
import static ru.yandex.realty.utils.RealtyUtils.getRandomUserRequestBody;

/**
 * Created by vicdev on 17.04.17.
 */
public class ApiSteps extends WebDriverSteps {

    public static final String ONLY_FOR_MONEY = "money";
    public static final String ONLY_FOR_PROMOTION = "promotion";

    @Inject
    private Vos2Adaptor vos2Adaptor;

    @Inject
    private BackRtAdaptor backRt;

    @Inject
    private PromocodeAdaptor promoApi;

    @Inject
    private PassportAdaptor passport;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private ApiSearcher apiSearcher;

    @Inject
    private ApiVos2 apiVos2;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private ApiRealtyBack apiRealtyBack;

    public Account createYandexAccount(Account account) {
        passportSteps.login(account);
        return account;
    }

    @Step("Проверяем, что лист подписок пустой")
    public void shouldSeeEmptySubscriptionList(String uid) {
        backRt.waitSubscription("uid", uid, equalTo(0));
    }

    public Account createVos2Account(Account account, AccountType type) {
        //в сервисе
        UserReq userReq = getRandomUserRequestBody(account.getId(), type.getValue());
        if (Optional.ofNullable(account.getPhone()).isPresent()) {
            userReq.withTelephones(newArrayList(account.getPhone().get()));
        }
        vos2Adaptor.createVos2Account(userReq);
        passportSteps.login(account);
        return account;
    }

    //Свой UserReq
    public Account createVos2Account(Account account, UserReq userReq) {
        if (Optional.ofNullable(account.getPhone()).isPresent()) {
            userReq.withTelephones(newArrayList(account.getPhone().get()));
        }
        vos2Adaptor.createVos2Account(userReq);
        passportSteps.login(account);
        return account;
    }

    @Step("Создаем аккаунт для юридического лица (дефолт Агентство)")
    public Account createRealty3JuridicalAccount(Account account) {
        createRealty3JuridicalAccountWithType(account, "AGENCY");
        return account;
    }

    @Step("Создаем аккаунт для юридического лица с типом {type}")
    public Account createRealty3JuridicalAccountWithType(Account account, String type) {
        UserContacts userContacts = getObjectFromJson(JuridicalUserBody.class, "realty3/juridical_person.json")
                .getUserContacts();
        userContacts.setEmail(getRandomEmail());
        userContacts
                .setEmail(getRandomEmail())
                .setName(account.getName());
        if (Optional.ofNullable(account.getPhone()).isPresent()) {
            userContacts.setPhones(newArrayList(new Phone().setWholePhoneNumber(account.getPhone().get())));
        }
        JuridicalUserBody body = getObjectFromJson(JuridicalUserBody.class, "realty3/juridical_person.json");
        body.setUserContacts(userContacts);
        body.setUserInfo(body.getUserInfo().setUserType(type));
        retrofitApiSteps.juridicalUser(account.getId(), body);
        passportSteps.login(account);
        return account;
    }

    @Step("Создаём аккаунт в VOS")
    public Account createVos2AccountWithoutLogin(Account account, AccountType type) {
        //в сервисе
        UserReq userReq = getRandomUserRequestBody(account.getId(), type.getValue());
        if (Optional.ofNullable(account.getPhone()).isPresent()) {
            userReq.withTelephones(newArrayList(account.getPhone().get()));
        }
        vos2Adaptor.createVos2Account(userReq);
        return account;
    }

    @Step("Логинимся через Акву")
    public void login(Account account) {
        passportSteps.login(account);
    }

    @Step("Проверяем, что в VOS2 у пользователя есть оффер с нужными полями")
    public OfferAssert shouldSeeLastOffer(Account account) {
        UserOffersResp userOffersResp = vos2Adaptor.waitUserOffers(account.getId());
        String id = userOffersResp.getOffers().get(0).getId();
        OfferInfo res = vos2Adaptor.getOffer(account.getId(), id);
        return assertThat(res.getOffer());
    }

    @Step("Возращаем первый оффер по uid")
    public OfferInfo getOfferInfo(Account account) {
        UserOffersResp userOffersResp = vos2Adaptor.waitUserOffers(account.getId());
        String id = userOffersResp.getOffers().get(0).getId();
        return vos2Adaptor.getOffer(account.getId(), id);
    }

    @Step("Проверяем, что в VOS2 есть пользователь с нужными полями")
    public UserAssert shouldSeeAccountInVos2(Account account) {
        GetUserResp userResp = vos2Adaptor.getVos2Account(account.getId());
        return assertThat(userResp.getUser());
    }

    @Step("Проверяем, что в VOS2 есть пользователь с нужными полями")
    public String getUserEmail(Account account) {
        return vos2Adaptor.getVos2Account(account.getId()).getUser().getEmail();
    }

    @Step("Проверяем, что есть подписка с нужными полями uid для email")
    public EmailAssert shouldSeeLastSubscriptionEmailByUid(String uid) {
        String prefix = "uid";
        backRt.waitSubscription(prefix, uid);
        return EmailAssert.assertThat(backRt.getSubscriptions("uid", uid).get(0).getDelivery().getEmail());
    }

    @Step("Проверяем, что есть подписка с нужными полями uid")
    public SubscriptionsRespAssert shouldSeeLastSubscriptionByUid(String uid) {
        String prefix = "uid";
        backRt.waitSubscription(prefix, uid);
        return SubscriptionsRespAssert.assertThat(backRt.getSubscriptions("uid", uid).get(0));
    }

    @Step("Проверяем, что есть подписка с нужными полями uid для email")
    public EmailAssert shouldSeeLastSubscriptionEmailByYandexUid(String yandexuid) {
        String prefix = "yandexuid";
        backRt.waitSubscription(prefix, yandexuid);
        return EmailAssert.assertThat(backRt.getSubscriptions(prefix, yandexuid).get(0).getDelivery().getEmail());
    }

    @Step("Ждем, чтобы статус оффера в серчере стал «inactive»")
    public void waitSearcherOfferStatusInactive(String oid) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await()
                .pollInterval(1, SECONDS).atMost(50, SECONDS).alias("Оффер должен стать «inactive»")
                .untilAsserted(() -> assertThat(apiSearcher.cardjson().withId(oid)
                        .get(validatedWith(shouldBe200Ok())).as(Card.class, GSON).getData()
                        .get(0).getOffers().get(0)).hasActive(false));
    }

    @Step("Ждем дезактивации оффера")
    public void waitOfferInactive(String uid, String oid) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().untilAsserted(()
                -> assertThat(apiVos2.offer().userID().withUserID(uid).offerID().withOfferID(oid)
                .get(validatedWith(shouldBeStatusOk())).as(OfferInfo.class, GSON).getOffer())
                .hasStatus(INACTIVE.toString()));
    }

    @Step("Ждем оффер «{matcher}»")
    public void waitFirstOffer(Account account, Matcher<OfferInfo> matcher) {
        UserOffersResp userOffersResp = vos2Adaptor.waitUserOffers(account.getId());
        String id = userOffersResp.getOffers().get(0).getId();
        vos2Adaptor.waitOffer(account.getId(), id, matcher);
    }


    @Step("Получаем список офферов «{account}»")
    public List<String> offerIds(Account account) {
        UserOffersResp userOffersResp = vos2Adaptor.waitUserOffers(account.getId());
        return userOffersResp.getOffers().stream().map(offer -> offer.getId()).collect(toList());
    }

    @Step("Должны видеть количество офферов «{matcher}»")
    public ApiSteps shouldSeeOfferCount(Account account, Matcher matcher) {
        Assert.assertThat("Ожидали другое количество оферов", offerIds(account), matcher);
        return this;
    }

    @Step("Ждем удаления оффера")
    public void waitOfferDeleted(String uid, String oid) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().untilAsserted(()
                -> assertThat(apiVos2.offer().userID().withUserID(uid).offerID().withOfferID(oid)
                .get(Function.identity()).as(OfferInfo.class, GSON)).hasStatus("ERROR"));
    }

    @Step("Ждём удаления всех офферов")
    public void shouldUserOffersDeleted(Account account) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().untilAsserted(
                () -> Java6Assertions.assertThat(vos2Adaptor.getUserOffers(account.getId()).getOffers()).isEmpty());
    }


    @Step("Ждем статус оффера")
    public void waitOfferStatus(String uid, String oid, String status) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().untilAsserted(()
                -> assertThat(apiVos2.offer().userID().withUserID(uid).offerID().withOfferID(oid)
                .get(Function.identity()).as(OfferInfo.class, GSON)).hasStatus(status));
    }

    @Step("Ждем обновления цены на первый оффер на {price}")
    public void waitOfferPrice(String uid, long price) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).untilAsserted(
                () -> assertThat(apiVos2.userOffers().userID().withUserID(uid).withDefaults()
                        .get(validatedWith(shouldBeStatusOk())).as(UserOffersResp.class, GSON)
                        .getOffers().get(0)).hasPrice(price));
    }

    @Step("Проверяем, что значение поля даты обновления оффера обновилось")
    public void waitOfferUpdateTimeChange(String uid, int index, String oldTime) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().untilAsserted(
                () -> assertThat(apiVos2.userOffers().userID().withUserID(uid).withDefaults()
                        .get(validatedWith(shouldBeStatusOk())).as(UserOffersResp.class).getOffers().get(index))
                        .matches(offer -> !offer.getUpdateTime().equals(oldTime)));
    }

    @Step("Создание подписки")
    public void createSubscription(String uid, SubscriptionQualifier qualifier) {
        backRt.createSubscription("uid", uid, qualifier);
    }

    @Step("Создание подписки")
    public void createSubscriptionWithReq(String uid, SubscriptionQualifier qualifier, String body, String title,
                                          String email) {
        backRt.createSubscriptionWithReq("uid", uid, qualifier, body, title, email);
    }

    @Step("Создание выключенной подписки")
    public void createDisabledSubscription(String uid, SubscriptionQualifier qualifier) {
        backRt.createDisabledSubscription("uid", uid, qualifier);
    }

    @Step("Создание подписки с email, который не подтвержён")
    public void createNotConfirmSubscription(String uid, String email) {
        backRt.createNotConfirmSubscription("uid", uid, email);
    }

    /**
     * Получение кода для подтверждения телефона по смс
     */
    public String getConfirmationCode(String trackId) {
        return passport.getConfirmationCode(trackId);
    }

    public List<String> createOffers(Account account, List<Offer> offerList) {
        List<String> ids = vos2Adaptor.createOffers(account.getId(), offerList).getId();
        return ids;
    }

    public void createDraft(Account account, List<ru.auto.test.api.realty.draft.create.userid.Offer> offer) {
        vos2Adaptor.createDraft(account.getId(), offer);
    }

    public void waitBanOffer(Account account, String offerId) {
        vos2Adaptor.waitBanOffer(account.getId(), offerId);
    }

    public void deleteOffer(Account account, String offerId) {
        vos2Adaptor.deleteOffer(account.getId(), offerId);
    }

    @Step("Создаём промокод")
    public ApiSteps createPromocode(CreatePromoBody promoBody) {
        promoApi.createPromocode(promoBody);
        return this;
    }

    @Step("Применяем промокод «{promoName}»")
    public ApiSteps applyPromocode(String promoName, String uid) {
        promoApi.applyPromocode(promoName, uid);
        return this;
    }

    @Step("Создаем и применяем промокод c {vas} для id:{uid}, ")
    public void createAndApplyPromocode(String vas, String uid) {
        String code = getRandomString();
        promoApi.createPromocode(defaultPromo().withCode(code)
                .withFeatures(asList(promoFeature()
                        .withCount(1L)
                        .withTag(vas)))
                .withConstraints(promoConstrains()));
        promoApi.applyPromocode(code, uid);
    }

    @Step("Создаем и применяем промокод на 200р для id:{uid}, ")
    public void createAndApplyMoneyPromocode(String uid) {
        String firstCode = getRandomString();
        promoApi.createPromocode(defaultPromo().withCode(firstCode).withFeatures(asList(promoFeature()
                .withCount(200L)
                .withTag(ONLY_FOR_MONEY)))
                .withConstraints(promoConstrains()));
        promoApi.applyPromocode(firstCode, uid);
    }

    @Step("Проверяем значение 'userNote' для оффера {offerId}")
    public void shouldSeeUserNote(String uid, String offerId, Matcher<String> matcher) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().until(() ->
                apiSearcher.cardjson().withId(offerId).withLogin(uid)
                        .get(validatedWith(ResponseSpecBuilders.shouldBe200Ok())).as(Card.class, GSON)
                        .getData().get(0).getOffers().get(0).getUserNote(), matcher);
    }

    @Step("Проверяем отсутствие поля 'userNote' у оффера: {offerId}")
    public void shouldNotSeeUserNote(String uid, String offerId) {
        shouldSeeUserNote(uid, offerId, nullValue(String.class));
    }

    @Step("Ждем активацию услуги продвижения первого оффера у пользователя «{uid}»")
    public void waitBePromotedFirstOffer(Account account) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await()
                .untilAsserted(() ->
                        Assert.assertThat("Продвижение не активировано", getOfferInfo(account)
                                .getOffer().getPromotion().getStatus(), is("active")));

    }

    @Step("Ждем активацию услуги премиум первого оффера у пользователя «{uid}»")
    public void waitBePremiumFirstOffer(Account account) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await()
                .pollInterval(3, SECONDS).atMost(30, SECONDS)
                .untilAsserted(() ->
                        Assert.assertThat("Премиум не активирован", getOfferInfo(account)
                                .getOffer().getPremium().getStatus(), is("active")));
    }

    @Step("Обновляем цену первого оффера")
    public void updatePrice(Account account, Long price, OfferType offerType) {
        String firstOfferId = getOfferInfo(account).getOffer().getId();
        vos2Adaptor.updatePrice(account.getId(), firstOfferId, price, offerType);
        waitOfferPrice(account.getId(), price);
    }

    @Step("Проверяем наличие премиума у оффера")
    public void checkOfferPremiumEnabled(String offerId) {
        MatcherAssert.assertThat(String.format("Проверяем наличие премиума у оффера %s", offerId),
                apiSearcher.cardjson().withId(offerId).get(validatedWith(shouldBe200Ok())).as(Card.class, GSON)
                        .getData().get(0).getOffers().get(0).getPremium(),
                equalTo(true));
    }

    @Step("Проверяем подписка юзера {uid} в статусе {status}")
    public void checkSubscriptionInStatus(String uid, SubscriptionStatus status) {
        await().pollInterval(2, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    SubscriptionsResp resp = Arrays.asList(apiRealtyBack.service().withDefaults().user().withUser(
                            String.format("uid:%s", uid)).subscriptions().get(validatedWith(shouldBe200Ok()))
                            .as(SubscriptionsResp[].class)).get(0);
                    SubscriptionsRespAssert.assertThat(resp).hasState(status.value());
                });
    }

}
