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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_PRIVATE_SELLER;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserInfo.userInfo;
import static ru.auto.tests.desktop.mock.MockUserOffers.USER_ID;
import static ru.auto.tests.desktop.mock.MockUserOffers.resellerOffersExample;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка оффера перекупа")
@Epic(AutoruFeatures.RESELLER_PUBLIC_PROFILE)
@Feature("Провязки с карточкой оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ResellerOfferCardTest {

    private static final String SALE_ID = getRandomOfferId();

    private static final int CARS_COUNT = getRandomShortInt();
    private static final int MOTO_COUNT = getRandomShortInt();
    private static final int TRUCKS_COUNT = getRandomShortInt();

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
                                userInfo()
                                        .setOffersCount("CARS", CARS_COUNT, getRandomShortInt())
                                        .setOffersCount("MOTO", MOTO_COUNT, getRandomShortInt())
                                        .setOffersCount("TRUCKS", TRUCKS_COUNT, getRandomShortInt())
                                        .getBody()),
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse())
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кол-во активных офферов продавца на карточке")
    public void shouldSeeSellerActiveOffersCount() {
        basePageSteps.onCardPage().contacts().offersCount().should(hasText(
                format(" · %d в продаже",
                        CARS_COUNT + MOTO_COUNT + TRUCKS_COUNT)
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Город продавца, при указанном адресе")
    public void shouldSeeSellerCityWithAddress() {
        basePageSteps.onCardPage().contacts().location().should(hasText(MOSCOW));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Город продавца, при указанном метро")
    public void shouldSeeSellerCityWithMetro() {
        mockRule.overwriteStub(2,
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_PRIVATE_SELLER)
                                        .setMetroFili()
                                        .setEncryptedUserId(USER_ID)
                                        .setId(SALE_ID).getResponse()));

        urlSteps.refresh();

        basePageSteps.onCardPage().contacts().location().should(hasText(MOSCOW));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст тултипа с кол-вом активных офферов продавца на карточке")
    public void shouldSeeSellerActiveOffersCountInTooltip() {
        basePageSteps.onCardPage().contacts().sellerName().hover();

        basePageSteps.onCardPage().resellerTooltip().should(
                hasText("Профессионально занимается покупкой и продажей автомобилей.")
        );
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на публичный профиль продавца с имени продавца на карточке")
    public void shouldGoToSellerPublicProfileFromName() {
        basePageSteps.onCardPage().contacts().sellerName().click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на публичный профиль продавца с кол-ва офферов на карточке")
    public void shouldGoToSellerPublicProfileFromOffersCount() {
        basePageSteps.onCardPage().contacts().offersCount().click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(RESELLER).path(USER_ID).path(ALL).path(SLASH).shouldNotSeeDiff();
    }

}
