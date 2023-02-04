package ru.auto.tests.mobile.dealers;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг дилеров - выбор марки")
@Feature(DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingMarksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(NEW).open();
        basePageSteps.onDealersListingPage().filters().button("Марка").click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки из списка популярных")
    public void shouldSelectMarkFromPopular() {
        String mark = "Audi";
        basePageSteps.onDealersListingPage().mmmPopup().popularMark(mark).click();
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(mark.toLowerCase()).path(NEW).shouldNotSeeDiff();
        basePageSteps.onDealersListingPage().filters().button(mark).waitUntil(isDisplayed());
        basePageSteps.onDealersListingPage().dealersList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки из списка всех")
    public void shouldSelectMarkFromAll() {
        String mark = "Audi";
        basePageSteps.onDealersListingPage().mmmPopup().allMark(mark).click();
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(mark.toLowerCase()).path(NEW)
                .shouldNotSeeDiff();
        basePageSteps.onDealersListingPage().filters().button(mark).waitUntil(isDisplayed());
        basePageSteps.onDealersListingPage().dealersList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск марки")
    public void shouldSearchMark() {
        String mark = "Audi";
        basePageSteps.onDealersListingPage().mmmPopup().input("Поиск марки", mark.toLowerCase());
        basePageSteps.onDealersListingPage().mmmPopup().marksList().waitUntil(hasSize(1)).get(0).click();
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(mark.toLowerCase()).path(NEW).shouldNotSeeDiff();
        basePageSteps.onDealersListingPage().filters().button(mark).waitUntil(isDisplayed());
        basePageSteps.onDealersListingPage().dealersList().waitUntil(hasSize(greaterThan(0)));
    }

}
