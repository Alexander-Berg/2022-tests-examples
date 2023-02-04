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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_GEO_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг дилеров - сброс гео-радиуса")
@Feature(DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingGeoRadiusResetTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс радиуса")
    public void shouldResetRadius() {
        basePageSteps.onDealersListingPage().filters().button("+ 200 км").resetButton().click();
        urlSteps.shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue("gradius", "0");
        cookieSteps.shouldSeeCookieWithValue("gids", MOSCOW_GEO_ID);
        basePageSteps.onDealersListingPage().filters().button("+ 0 км").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Обновление списка дилеров после сброса радиуса")
    public void shouldRefreshDealersListAfterReset() {
        String dealersCount = basePageSteps.onDealersListingPage().dealersCount().getText();
        basePageSteps.onDealersListingPage().filters().button("+ 200 км").resetButton().click();
        urlSteps.ignoreParam("cookiesync").shouldNotSeeDiff();
        basePageSteps.onDealersListingPage().dealersCount().waitUntil(not(hasText(dealersCount)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Обновление списка дилеров после смены радиуса")
    public void shouldRefreshDealersListAfterChange() {
        String dealersCount = basePageSteps.onDealersListingPage().dealersCount().getText();
        basePageSteps.onDealersListingPage().filters().button("+ 200 км").click();
        basePageSteps.onDealersListingPage().geoRadiusPopup().geoRadius("1000 км").click();
        urlSteps.ignoreParam("cookiesync").shouldNotSeeDiff();
        basePageSteps.onDealersListingPage().dealersCount().waitUntil(not(hasText(dealersCount)));
    }
}
