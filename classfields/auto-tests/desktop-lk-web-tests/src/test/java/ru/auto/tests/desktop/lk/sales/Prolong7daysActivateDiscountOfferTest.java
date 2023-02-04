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

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Notifications.OFFER_ACTIVATED;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.element.BillingPopup.PAYMENT_SUCCESS;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
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
public class Prolong7daysActivateDiscountOfferTest {

    private static final String ID = "1076842087-f1e84";

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
                stub("desktop-lk/UserOffersCarsInactiveProlongableTrueDiscount"),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT)
                        .withRequestQuery(
                                query().setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(
                                offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Активация неактивного объявления со скидкой")
    public void shouldActivateSaleWithDiscount() {
        mockRule.setStubs(stub("desktop/UserWithTiedCard"),
                stub("desktop-lk/BillingAutoruPaymentInitDiscount"),
                stub("desktop-lk/BillingAutoruPaymentProcessTiedCard"),
                stub("desktop/BillingAutoruPayment"),
                stub().withGetDeepEquals(format("%s/%s", USER_OFFERS_CARS, ID))
                        .withResponseBody(
                                mockOffer(CAR_EXAMPLE).setId(ID).getResponse())
        ).update();

        basePageSteps.onLkSalesPage().autoProlongDiscountBanner().title()
                .should(hasText("Продлите размещение со скидкой 43%"));
        basePageSteps.onLkSalesPage().autoProlongDiscountBanner().priceInfo()
                .should(hasText("Стоимость продления\n2 349 ₽\n1 349 ₽\nВыгода 43%"));
        basePageSteps.onLkSalesPage().autoProlongDiscountBanner().buttonContains("Продлить за").click();
        basePageSteps.onLkSalesPage().billingPopupFrame()
                .waitUntil("Ожидаем загрузки фрейма поп-апа платежа", isDisplayed(), 10);
        basePageSteps.onLkSalesPage().switchToBillingFrame();
        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().billingPopup().tiedCardPayButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().billingPopup().successMessage().waitUntil(hasText(PAYMENT_SUCCESS));
        basePageSteps.switchToDefaultFrame();

        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText(OFFER_ACTIVATED));
        basePageSteps.onLkSalesPage().billingPopupCloseButton().click();
        basePageSteps.onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
    }
}
