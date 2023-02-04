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

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SPB;
import static ru.auto.tests.desktop.consts.QueryParams.COOKIESYNC;
import static ru.auto.tests.desktop.consts.Regions.SPB_GEO_ID;
import static ru.auto.tests.desktop.step.CookieSteps.GIDS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг дилеров - регион")
@Feature(DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingRegionTest {

    private static final String REGION = "Санкт-Петербург";
    private static final String PARENT_REGION = "Санкт-Петербург и Ленинградская область";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(NEW).open();
        basePageSteps.onDealersListingPage().filters().button("Москва").click();
        basePageSteps.onDealersListingPage().geoPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск региона")
    public void shouldSearchRegion() {
        basePageSteps.onDealersListingPage().geoPopup().resetButton().click();
        basePageSteps.onDealersListingPage().geoPopup().searchRegionButton().click();
        basePageSteps.onDealersListingPage().geoSuggestPopup().input("Населённый пункт", REGION);
        basePageSteps.onDealersListingPage().geoSuggestPopup().regionsList().waitUntil(hasSize(2));
        basePageSteps.onDealersListingPage().geoSuggestPopup().getRegion(1).click();
        basePageSteps.onDealersListingPage().geoPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onDealersListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue(GIDS, SPB_GEO_ID);
        basePageSteps.onDealersListingPage().filters().button(REGION).waitUntil(isDisplayed());
        urlSteps.testing().path(SPB).path(DILERY).path(CARS).path(NEW).ignoreParam(COOKIESYNC).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор региона")
    public void shouldSelectRegion() {
        basePageSteps.onDealersListingPage().geoPopup().resetButton().click();
        basePageSteps.onDealersListingPage().geoPopup().regionGroup(PARENT_REGION)
                .arrowButton().click();
        basePageSteps.onDealersListingPage().geoPopup().region(REGION).click();
        basePageSteps.onDealersListingPage().geoPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onDealersListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue(GIDS, SPB_GEO_ID);
        basePageSteps.onDealersListingPage().filters().button(REGION).waitUntil(isDisplayed());
        urlSteps.testing().path(SPB).path(DILERY).path(CARS).path(NEW).ignoreParam(COOKIESYNC).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class})
    @DisplayName("Выбор двух регионов")
    public void shouldSelectTwoRegions() {
        String secondRegion = "Бокситогорск";
        String secondRegionId = "10861";

        basePageSteps.onDealersListingPage().geoPopup().resetButton().click();
        basePageSteps.onDealersListingPage().geoPopup().regionGroup(PARENT_REGION)
                .arrowButton().click();
        basePageSteps.onDealersListingPage().geoPopup().region(REGION).click();
        basePageSteps.onDealersListingPage().geoPopup().region(secondRegion).click();
        basePageSteps.onDealersListingPage().geoPopup().title().waitUntil(hasText("Выбрано 2"));
        basePageSteps.onDealersListingPage().geoPopup().readyButton().click();
        basePageSteps.onDealersListingPage().filters().button(format("%s + 1", REGION)).waitUntil(isDisplayed());

        cookieSteps.shouldSeeCookieWithValue(GIDS, format("%s%%2C%s", SPB_GEO_ID, secondRegionId));
        urlSteps.testing().path(DILERY).path(CARS).path(NEW).shouldNotSeeDiff();
    }

}
