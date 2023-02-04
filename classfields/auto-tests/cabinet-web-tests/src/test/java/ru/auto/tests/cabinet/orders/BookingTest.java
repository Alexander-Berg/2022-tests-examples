package ru.auto.tests.cabinet.orders;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.BOOKING;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.ORDERS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Бронирование")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class BookingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                        "desktop/SearchCarsBreadcrumbs",
                        "cabinet/ApiAccessClient",
                        "cabinet/CommonCustomerGet",
                        "cabinet/Booking",
                        "cabinet/BookingPage2",
                        "cabinet/OfferCarsPhones")
                .post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(ORDERS).path(BOOKING).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение заявки")
    public void shouldSeeBookingItem() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetBookingPage().getBookingItem(0));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetBookingPage().getBookingItem(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению в заявке")
    public void shouldClickOffer() {
        steps.onCabinetBookingPage().getBookingItem(0).title().click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP)
                .path("/bmw/6er/21046310/21185512/1098488192-c50fa9c6/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке телефона в заявке")
    public void shouldClickPhoneButton() {
        steps.onCabinetBookingPage().getBookingItem(0).phoneButton().click();
        steps.onCabinetBookingPage().getBookingItem(0).phoneButton().waitUntil(hasText("+7 925 509-69-18"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Статус «Просрочен»")
    public void shouldSeeExpiredStatus() {
        steps.onCabinetBookingPage().getBookingItem(0).status("Просрочен").hover();
        steps.onCabinetBookingPage().activePopup().should(hasText("Статус «Просрочен»\nПользователь в период " +
                "бронирования не явился в салон за автомобилем."));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Статус «Не подтверждён»")
    public void shouldSeeUnconfirmedStatus() {
        steps.onCabinetBookingPage().getBookingItem(1).status("Не подтверждён").hover();
        steps.onCabinetBookingPage().activePopup().should(hasText("Статус «Не подтверждён»\nЗаявка в период бронирования " +
                "была отменена вами."));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Статус «Продан»")
    public void shouldSeeSoldStatus() {
        steps.onCabinetBookingPage().getBookingItem(2).status("Продан").hover();
        steps.onCabinetBookingPage().activePopup().should(hasText("Статус «Продан»\nАвтомобиль был продан по данной " +
                "заявке на бронирование."));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Статус «Отменён»")
    public void shouldSeeCanceledStatus() {
        steps.onCabinetBookingPage().getBookingItem(3).status("Отменён").hover();
        steps.onCabinetBookingPage().activePopup().should(hasText("Статус «Отменён»\nЗаявка в период бронирования была " +
                "отменена вами."));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по иконке ?")
    public void shouldClickHelpIcon() {
        steps.onCabinetBookingPage().getBookingItem(0).helpIcon().hover();
        steps.onCabinetBookingPage().activePopup().should(hasText("Цена автомобиля на момент бронирования"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пагинатор")
    public void shouldClickNextPage() {
        steps.onCabinetBookingPage().pager().page("2").should(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        steps.onCabinetBookingPage().bookingItemsList().waitUntil(hasSize(10));
        steps.onCabinetBookingPage().pager().currentPage().waitUntil(hasText("2"));
        steps.onCabinetBookingPage().getBookingItem(0).title().waitUntil(hasText("Toyota Camry VIII (XV70)"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пагинатор, Показать ещё")
    public void shouldClickShowMoreButton() {
        steps.onCabinetBookingPage().bookingItemsList().waitUntil(hasSize(10));
        steps.onCabinetBookingPage().pager().button("Показать ещё").should(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        steps.onCabinetBookingPage().bookingItemsList().waitUntil(hasSize(20));
        steps.onCabinetBookingPage().pager().currentPage().waitUntil(hasText("2"));
    }
}
