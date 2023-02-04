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
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BOOKING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Бронирование в листинге")
@Feature(BOOKING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BookingListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsAll").post();

        urlSteps.testing().path(CARS).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение статуса бронирования (можно забронировать)")
    public void shouldSeeBookingAllowedStatus() {
        basePageSteps.onListingPage().getSale(3).bookingStatus("Можно забронировать").hover();
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed()).should(hasText("Забронируйте сейчас, " +
                "заберите потом\nАвтомобиль будет ждать вас в салоне в течение 3 дней. Цена не изменится и останется " +
                "такой же, как в объявлении. Если передумаете или не успеете забрать машину, мы вернём вам полную сумму " +
                "бронирования."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение статуса бронирования (автомобиль забронирован)")
    public void shouldSeeAlreadyBookedStatus() {
        basePageSteps.onListingPage().getSale(4).hover();
        basePageSteps.onListingPage().getSale(4).bookingStatus("Автомобиль забронирован").hover();
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed()).should(hasText("Автомобиль забронирован " +
                "до 30 декабря 2025\nДругой пользователь оформил бронь на эту машину. " +
                "Если он не выкупит её до 30 декабря 2025, она снова будет доступна. Добавьте объявление в избранное, " +
                "чтобы следить за ним.\n\nИщите отметку «Можно забронировать» в объявлениях, чтобы зафиксировать " +
                "цену на машину и «закрепить» её за собой на 3 дня."));
    }
}