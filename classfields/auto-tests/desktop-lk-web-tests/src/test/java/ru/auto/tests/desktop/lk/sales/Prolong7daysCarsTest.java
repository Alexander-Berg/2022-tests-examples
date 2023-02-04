package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFI;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_ACTIVATE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.getDateDaysFromNow;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffer.service;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Личный кабинет - продление 7 дней")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@Story("Продление 7 дней")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class Prolong7daysCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Отображение на сниппете в объявлении x дней до продления / ххх ₽ в неделю")
    public void shouldSeeSnippet() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)
                                        .setExpireDate(getDateDaysFromNow(8))
                                        .setServices(
                                                service(ALL_SALE_ACTIVATE)
                                                        .setProlongable(true)
                                                        .setAutoProlongPrice(1999)
                                                        .setDays(7)
                                                        .setProlongationForcedNotTogglable(true))
                        ).build())
        ).update();


        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesPage().getSale(0).autoProlongInfo().should(hasText("Продление:\nчерез 7 дней\n" +
                "Стоимость продления:\n1 999 \u20BD / 7 дней"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа при наведении на «количество дней до продления»")
    public void shouldSeePopup() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)
                                        .addProlongationAllowedForServicePrice(ALL_SALE_ACTIVATE)
                                        .setServices(
                                                service(ALL_SALE_ACTIVATE)
                                                        .setExpireDate(getDateDaysFromNow(7))
                                                        .setProlongable(true)
                                                        .setAutoProlongPrice(1999)
                                                        .setDays(7)
                                                        .setProlongationForcedNotTogglable(true))
                        ).build())
        ).update();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesPage().getSale(0).autoProlongInfo().timeLeft().waitUntil(isDisplayed()).hover();
        basePageSteps.onLkSalesPage().activePopup().should(hasText("Размещение на 7 дней\n" +
                "Стоимость размещения стала ниже! Экономьте на быстрой продаже — каждую " +
                "неделю объявление будет автоматически обновляться и продлеваться за 1 999 ₽.\n" +
                "Подробнее"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке в поп-апе при наведении на «количество дней до продления»")
    public void shouldClickLinkInPopup() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)
                                        .addProlongationAllowedForServicePrice(ALL_SALE_ACTIVATE)
                                        .setServices(
                                                service(ALL_SALE_ACTIVATE)
                                                        .setExpireDate(getDateDaysFromNow(7))
                                                        .setProlongable(true)
                                                        .setAutoProlongPrice(1999)
                                                        .setDays(7)
                                                        .setProlongationForcedNotTogglable(true))
                        ).build())
        ).update();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesPage().getSale(0).autoProlongInfo().timeLeft().waitUntil(isDisplayed()).hover();
        basePageSteps.onLkSalesPage().activePopupLink().click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(DEALER).path(PROFI).shouldNotSeeDiff();
    }

}
