package ru.auto.tests.desktop.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.mountebank.http.predicates.PredicateType;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.consts.QueryParams.RESELLER_PUBLIC;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.noRequest;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_PRIVATE_SELLER;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserInfo.userInfo;
import static ru.auto.tests.desktop.mock.MockUserOffers.USER_ID;
import static ru.auto.tests.desktop.mock.MockUserOffers.resellerOffersExample;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;

@DisplayName("Метрики на карточке оффера перекупа")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@Feature("Провязки с карточкой оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class ResellerOfferCardMetricsTest {

    private static final String SALE_ID = getRandomOfferId();

    private static final String CARD_LINK_CLICK = "{\"cars\":{\"card\":{\"reseller_public\":{\"link-click\":{}}}}}";
    private static final String CARD_LINK_SHOW = "{\"cars\":{\"card\":{\"reseller_public\":{\"link-show\":{}}}}}";
    private static final String PHONE_SHOW = "{\"cars\":{\"card\":{\"show-phone\":{\"from_reseller_public\":{}}}}}";
    private static final String CARD_LINK_SHOW_SELLER_POPUP = "{\"cars\":{\"card\":{\"reseller_public\":{\"link-show\":{\"sellerPopup\":{}}}}}}";
    private static final String CARD_LINK_CLICK_SELLER_POPUP = "{\"cars\":{\"card\":{\"reseller_public\":{\"link-click\":{\"sellerPopup\":{}}}}}}";
    private static final String CARD_CHAT_OPEN = "{\"cars\":{\"card\":{\"chat_open\":{\"from_reseller_public\":{}}}}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s/offers/all", USER, USER_ID))
                        .withRequestQuery(
                                query().setStatus(ACTIVE.toUpperCase()))
                        .withResponseBody(
                                resellerOffersExample().build()),
                stub().withGetDeepEquals(format("%s/%s/info", USER, USER_ID))
                        .withResponseBody(
                                userInfo().getBody())
        );

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).path(SLASH);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «link-show» на карточке оффера, при показе ссылки на публичный профиль перекупа")
    public void shouldSeeLinkShowMetricOfferCard() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse())
        ).create();

        urlSteps.open();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(CARD_LINK_SHOW)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет метрики «link-show» на карточке оффера, без ссылки на публичный профиль перекупа")
    public void shouldNotSeeLinkShowMetricOfferCard() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setId(SALE_ID).getResponse())
        ).create();

        urlSteps.open();

        seleniumMockSteps.assertWithWaiting(noRequest(hasSiteInfo(CARD_LINK_SHOW)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «link-click», при переходе на публичный профиль перекупа с имени продавца на карточке")
    public void shouldSeeLinkClickMetricFromSellerName() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse())
        ).create();

        urlSteps.open();
        basePageSteps.onCardPage().contacts().sellerName().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(CARD_LINK_CLICK)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «link-click», при переходе на публичный профиль перекупа с кол-ва офферов на карточке")
    public void shouldSeeLinkClickMetricFromOffersCount() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse())
        ).create();

        urlSteps.open();
        basePageSteps.onCardPage().contacts().offersCount().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(CARD_LINK_CLICK)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «show-phone» по «Показать телефон» на карточке, после перехода с публичного профиля")
    public void shouldSeeShowPhoneMetricOfferCardFromPublicProfile() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse()),
                stub("cabinet/OfferCarsPhones").withPredicateType(PredicateType.EQUALS)
                        .withPath(format("%s/%s/phones", OFFER_CARS, SALE_ID))
        ).create();

        urlSteps.addParam(FROM, RESELLER_PUBLIC).open();

        basePageSteps.onCardPage().contacts().showPhoneButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(PHONE_SHOW)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет метрики «show-phone» по «Показать телефон» на карточке без «from=reseller_public»")
    public void shouldSeeNoShowPhoneMetricOfferCardWithoutFromPublicProfile() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse()),
                stub("cabinet/OfferCarsPhones").withPredicateType(PredicateType.EQUALS)
                        .withPath(format("%s/%s/phones", OFFER_CARS, SALE_ID))
        ).create();

        urlSteps.open();

        basePageSteps.onCardPage().contacts().showPhoneButton().click();

        seleniumMockSteps.assertWithWaiting(noRequest(hasSiteInfo(PHONE_SHOW)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «link-show» при показе ссылки на продавца в попапе телефона")
    public void shouldSeeLinkShowSellerPopup() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse()),
                stub("cabinet/OfferCarsPhones").withPredicateType(PredicateType.EQUALS)
                        .withPath(format("%s/%s/phones", OFFER_CARS, SALE_ID))
        ).create();

        urlSteps.open();

        basePageSteps.onCardPage().contacts().showPhoneButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(CARD_LINK_SHOW_SELLER_POPUP)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «link-click», при переходе на публичный профиль перекупа из попапа телефонов")
    public void shouldSeeLinkClickMetricSellerPopup() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse()),
                stub("cabinet/OfferCarsPhones").withPredicateType(PredicateType.EQUALS)
                        .withPath(format("%s/%s/phones", OFFER_CARS, SALE_ID))
        ).create();

        urlSteps.open();
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().seller().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(CARD_LINK_CLICK_SELLER_POPUP)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «chat_open» по «Написать» на карточке, после перехода с публичного профиля")
    public void shouldSeeChatOpenMetricOfferCardFromPublicProfile() {
        mockRule.setStubs(
                sessionAuthUserStub(),
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse())
        ).create();

        urlSteps.addParam(FROM, RESELLER_PUBLIC).open();

        basePageSteps.onCardPage().contacts().sendMessageButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(CARD_CHAT_OPEN)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет метрики «chat_open» по «Написать» на карточке, после перехода с публичного профиля")
    public void shouldSeeNoChatOpenMetricOfferCardWithoutFromPublicProfile() {
        mockRule.setStubs(
                sessionAuthUserStub(),
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse())
        ).create();

        urlSteps.open();

        basePageSteps.onCardPage().contacts().sendMessageButton().click();

        seleniumMockSteps.assertWithWaiting(noRequest(hasSiteInfo(CARD_CHAT_OPEN)));
    }

}
