package ru.auto.tests.mobile.booking;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BOOKING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.LIKE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Бронирование под зарегом")
@Feature(BOOKING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class BookingRegTest {

    private static final String PATH = "/kia/optima/21342125/21342344/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private PaymentSteps paymentSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SessionAuthUser",
                "desktop/OfferCarsNewDealerBooking",
                "desktop/BookingTermsCarsOfferId",
                "desktop/BillingBookingPaymentInit",
                "desktop/BillingBookingPaymentProcess",
                "desktop/BillingBookingPayment",
                "desktop/UserFavoritesCarsPost").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Бронирование под зарегом")
    public void shouldBook() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().bookingButton());
        basePageSteps.onCardPage().bookingPopup().button("Продолжить").click();
        basePageSteps.onCardPage().bookingPopup().input("ФИО").sendKeys("Тест Тестович Тестов");
        basePageSteps.onCardPage().bookingPopup().buttonContains("Забронировать за").click();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Добавлено в избранное"));
        basePageSteps.onCardPage().bookingPopup().waitUntil(hasText("Автомобиль успешно забронирован\nВы забронировали " +
                "автомобиль Kia Optima IV Рестайлинг. Мы отправили вам смс с подтверждением брони, покажите его " +
                "в дилерском центре для выкупа машины.\n\nМы добавили это объявление в «Избранное»,\nчтобы " +
                "вы не потеряли его.\nПосмотреть в избранном"));
        basePageSteps.onCardPage().bookingPopup().button("Посмотреть в избранном").click();
        urlSteps.testing().path(LIKE).shouldNotSeeDiff();
    }
}
