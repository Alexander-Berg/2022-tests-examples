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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Notifications.AUTOPROLONG_ACTIVATED;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
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
public class Prolong7daysBannerProlongTest {

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
                stub().withGetDeepEquals(USER_OFFERS_CARS).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(USER_OFFER_CAR_EXAMPLE)
                                        .setExpireDate(getDateDaysFromNow(8))
                                        .addProlongationForcedNotTogglable(ALL_SALE_ACTIVATE)
                                        .setServices(
                                                service(ALL_SALE_ACTIVATE)
                                                        .setProlongable(false)
                                                        .setAutoProlongPrice(1999)
                                                        .setDays(7)
                                                        .setProlongationForcedNotTogglable(true))
                        ).build()),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение баннера «Продление выключено»")
    public void shouldSeeAutoProlongFailedBanner() {
        basePageSteps.onLkSalesPage().getSale(0).autoProlongFailedBanner().should(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).autoProlongFailedBanner().title()
                .should(hasText("Продление выключено"));
        basePageSteps.onLkSalesPage().getSale(0).autoProlongFailedBanner().text()
                .should(hasText("Включите продление, иначе через 7 дней объявление будет снято с публикации."));
        basePageSteps.onLkSalesPage().getSale(0).autoProlongFailedBanner().retryButton()
                .should(hasText("Включить продление"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Активация автопродления объявления в баннере")
    public void shouldActivateAutoProlong() {
        basePageSteps.onLkSalesPage().getSale(0).autoProlongInfo()
                .should(hasText("До снятия:\n7 дней\nСтоимость продления:\n1 999 \u20BD / 7 дней"));

        mockRule.overwriteStub(2, stub("desktop-lk/UserOffersCarsProlongableTrue"));
        mockRule.setStubs(stub("desktop-lk/UserOffersCarsProductAllSaleActivateProlongable")).update();

        basePageSteps.onLkSalesPage().getSale(0).autoProlongFailedBanner().retryButton().waitUntil(isDisplayed())
                .click();

        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed()).should(hasText(AUTOPROLONG_ACTIVATED));
        basePageSteps.onLkSalesPage().getSale(0).autoProlongInfo().should(hasText("Продление:\nчерез 7 дней\n" +
                "Стоимость продления:\n1 999 \u20BD / 7 дней"));
        basePageSteps.onLkSalesPage().getSale(0).autoProlongFailedBanner().waitUntil(not(isDisplayed()));
    }

}
