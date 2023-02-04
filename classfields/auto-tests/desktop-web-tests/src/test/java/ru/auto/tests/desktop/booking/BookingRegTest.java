package ru.auto.tests.desktop.booking;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BOOKING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Бронирование под зарегом")
@Feature(BOOKING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
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
    private YaKassaSteps yaKassaSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/SessionAuthUser",
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
        basePageSteps.onCardPage().bookingButton().click();
        basePageSteps.onCardPage().bookingPopup().input("ФИО", "Тест Тестович Тестов");
        basePageSteps.onCardPage().bookingPopup().buttonContains("Забронировать за").click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().header().waitUntil(hasText("Бронирование автомобиля"));
        basePageSteps.onCardPage().billingPopup().priceHeader().waitUntil(hasText("20 000 \u20BD"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Добавлено в избранное"));
        basePageSteps.onCardPage().bookingPopup().waitUntil(isDisplayed()).should(hasText("Автомобиль успешно " +
                "забронирован\nВы забронировали автомобиль Kia Optima IV Рестайлинг. Мы отправили вам смс " +
                "с подтверждением брони, покажите его в дилерском центре для выкупа машины.\n\nМы добавили " +
                "это объявление в «Избранное»,\nчтобы вы не потеряли его.\nПосмотреть в избранном"));
        basePageSteps.onCardPage().bookingPopup().button("Посмотреть в избранном").click();
        basePageSteps.onCardPage().favoritesPopup().waitUntil(isDisplayed());
    }
}
