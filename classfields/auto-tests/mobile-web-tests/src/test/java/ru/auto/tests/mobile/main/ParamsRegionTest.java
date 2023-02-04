package ru.auto.tests.mobile.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.auto.tests.desktop.consts.Pages.SPB;
import static ru.auto.tests.desktop.consts.Regions.DEFAULT_RADIUS;
import static ru.auto.tests.desktop.consts.Regions.RUSSIA_GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.SPB_GEO_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Регион")
@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ParamsRegionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().filters().paramsButton().click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск региона")
    public void shouldSearchRegion() {
        String region = "Санкт-Петербург";

        basePageSteps.onMainPage().paramsPopup().region().click();
        basePageSteps.onMainPage().geoPopup().resetButton().click();
        basePageSteps.onMainPage().geoPopup().searchRegionButton().click();
        basePageSteps.onMainPage().geoSuggestPopup().input("Населённый пункт", region);
        basePageSteps.onMainPage().geoSuggestPopup().regionsList().waitUntil(hasSize(2));
        basePageSteps.onMainPage().geoSuggestPopup().getRegion(1).click();
        basePageSteps.onMainPage().geoPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onMainPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", SPB_GEO_ID);
        basePageSteps.onMainPage().paramsPopup().region()
                .waitUntil(hasText(format("%s + %s км", region, DEFAULT_RADIUS)));
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(SPB).path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button(region).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор региона")
    public void shouldSelectRegion() {
        String region = "Санкт-Петербург";
        String parentRegion = "Санкт-Петербург и Ленинградская область";

        basePageSteps.onMainPage().paramsPopup().region().click();
        basePageSteps.onMainPage().geoPopup().resetButton().click();
        basePageSteps.onMainPage().geoPopup().regionGroup(parentRegion).arrowButton().click();
        basePageSteps.onMainPage().geoPopup().region(region).click();
        basePageSteps.onMainPage().geoPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onMainPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", "2");
        basePageSteps.onMainPage().paramsPopup().region()
                .waitUntil(hasText(format("%s + %s км", region, DEFAULT_RADIUS)));
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(SPB).path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button(region).waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Мультивыбор регионов")
    public void shouldMultiSelectRegions() {
        String region1 = "Москва";
        String regionCode1 = "213";
        String region2 = "Санкт-Петербург";
        String regionCode2 = "2";
        String parentRegion2 = "Санкт-Петербург и Ленинградская область";

        basePageSteps.onMainPage().paramsPopup().region().click();
        basePageSteps.onMainPage().geoPopup().regionGroup(parentRegion2).arrowButton().click();
        basePageSteps.onMainPage().geoPopup().region(region2).click();
        basePageSteps.onMainPage().geoPopup().title().waitUntil(hasText("Выбрано 2"));
        basePageSteps.onMainPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", format("%s%%2C%s", regionCode1, regionCode2));
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().region().waitUntil(hasText(format("%s, %s", region1, region2)));
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button(format("%s + 1", region1)).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор России")
    public void shouldSelectRussia() {
        String region = "Россия";

        basePageSteps.onMainPage().paramsPopup().region().click();
        basePageSteps.onMainPage().geoPopup().region(region).click();
        basePageSteps.onMainPage().geoPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onMainPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", RUSSIA_GEO_ID);
        basePageSteps.onMainPage().paramsPopup().radiusDisabled("+100 км").waitUntil(isDisplayed());
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().region().waitUntil(hasText(region));
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(RUSSIA).path(CARS).path(ALL).ignoreParam("cookiesync").shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button(region).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс региона")
    public void shouldResetRegion() {
        String region = "Москва";
        String parentRegion = "Москва и Московская область";

        basePageSteps.onMainPage().paramsPopup().region().click();
        basePageSteps.onMainPage().geoPopup().regionGroup(parentRegion).arrowButton().click();
        basePageSteps.onMainPage().geoPopup().region(region).click();
        basePageSteps.onMainPage().geoPopup().title().waitUntil(hasText("Регион"));
        basePageSteps.onMainPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", "");
        basePageSteps.onMainPage().paramsPopup().radiusDisabled("+100 км").waitUntil(isDisplayed());
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().region().waitUntil(hasText("Любой регион"));
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button("Любой регион").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс региона по кнопке «Сбросить»")
    public void shouldResetRegionByResetButton() {
        basePageSteps.onMainPage().paramsPopup().region().click();
        basePageSteps.onMainPage().geoPopup().resetButton().click();
        basePageSteps.onMainPage().geoPopup().title().waitUntil(hasText("Регион"));
        basePageSteps.onMainPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", "");
        basePageSteps.onMainPage().paramsPopup().radiusDisabled("+100 км").waitUntil(isDisplayed());
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().region().waitUntil(hasText("Любой регион"));
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button("Любой регион").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор радиуса")
    public void shouldSelectRadius() {
        String radius = "100";

        basePageSteps.onMainPage().paramsPopup().radius(format("+%s км", radius)).click();
        basePageSteps.onMainPage().paramsPopup().radiusSelected(format("+%s км", radius)).waitUntil(isDisplayed());
        basePageSteps.onMainPage().paramsPopup().region().waitUntil(hasText(format("Москва + %s км", radius)));
        cookieSteps.shouldSeeCookieWithValue("gids", "213");
        cookieSteps.shouldSeeCookieWithValue("gradius", radius);
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().geoRadiusCounters().geoRadiusCounterActive(format("+ %s км", radius))
                .should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Закрытие поп-апа")
    public void shouldClosePopup() {
        basePageSteps.onMainPage().paramsPopup().region().click();
        basePageSteps.onMainPage().geoPopup().closeButton().click();
        basePageSteps.onMainPage().geoPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onMainPage().paramsPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Закрытие саджеста регионов")
    public void shouldCloseRegionSuggest() {
        basePageSteps.onMainPage().paramsPopup().region().click();
        basePageSteps.onMainPage().geoPopup().searchRegionButton().click();
        basePageSteps.onMainPage().geoSuggestPopup().cancelButton().click();
        basePageSteps.onMainPage().geoSuggestPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onMainPage().geoPopup().waitUntil(isDisplayed());
    }
}
