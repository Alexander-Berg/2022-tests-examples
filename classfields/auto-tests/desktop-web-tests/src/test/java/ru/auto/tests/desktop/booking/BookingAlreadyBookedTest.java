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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BOOKING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Бронирование - отображение статуса бронирования (забронировано не мной)")
@Feature(BOOKING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BookingAlreadyBookedTest {

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
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/BookingTermsCarsOfferId",
                "desktop/OfferCarsNewDealerAlreadyBooked",
                "desktop/OfferCarsRelated").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение статуса бронирования (забронировано не мной)")
    public void shouldSeeBookedNotByMeStatus() {
        basePageSteps.onCardPage().bookingStatus().status().should(hasText("Этот автомобиль забронирован. " +
                "Посмотрите похожие\nДругой пользователь оформил бронь на эту машину. Если сделка не состоится " +
                "до 30 декабря 2025, вы сможете купить этот автомобиль. Или выбрать другой."));
        basePageSteps.onCardPage().bookingButton().should(not(isDisplayed()));

    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению в похожих")
    public void shouldClickRelatedSale() {
        basePageSteps.onCardPage().bookingStatus().horizontalRelated().itemsList().should(hasSize(5));
        basePageSteps.onCardPage().bookingStatus().horizontalRelated().getItem(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/toyota/land_cruiser/1103691301-d02bfdba/")
                .shouldNotSeeDiff();
    }
}