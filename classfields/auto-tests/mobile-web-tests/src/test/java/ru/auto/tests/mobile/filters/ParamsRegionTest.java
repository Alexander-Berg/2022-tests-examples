package ru.auto.tests.mobile.filters;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.auto.tests.desktop.consts.Pages.SPB;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Regions.RUSSIA_GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.SPB_GEO_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Регион")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
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

    //@Parameter("Категория ТС")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String defaultRadius;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, "200"},
                {TRUCK, "500"},
                {MOTORCYCLE, "200"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск региона")
    public void shouldSearchRegion() {
        String region = "Санкт-Петербург";

        basePageSteps.onListingPage().paramsPopup().region().click();
        basePageSteps.onListingPage().geoPopup().resetButton().click();
        basePageSteps.onListingPage().geoPopup().searchRegionButton().click();
        basePageSteps.onListingPage().geoSuggestPopup().input("Населённый пункт", region);
        basePageSteps.onListingPage().geoSuggestPopup().regionsList().waitUntil(hasSize(2));
        basePageSteps.onListingPage().geoSuggestPopup().getRegion(1).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", SPB_GEO_ID);
        basePageSteps.onListingPage().paramsPopup().region()
                .waitUntil(hasText(format("%s + %s км", region, defaultRadius)));
        urlSteps.testing().path(SPB).path(category).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button(region).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор региона")
    public void shouldSelectRegion() {
        String region = "Санкт-Петербург";
        String parentRegion = "Санкт-Петербург и Ленинградская область";

        basePageSteps.onListingPage().paramsPopup().region().click();
        basePageSteps.onListingPage().geoPopup().resetButton().click();
        basePageSteps.onListingPage().geoPopup().regionGroup(parentRegion).arrowButton().click();
        basePageSteps.onListingPage().geoPopup().region(region).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", "2");
        basePageSteps.onListingPage().paramsPopup().region()
                .waitUntil(hasText(format("%s + %s км", region, defaultRadius)));
        urlSteps.testing().path(SPB).path(category).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
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

        basePageSteps.onListingPage().paramsPopup().region().click();
        basePageSteps.onListingPage().geoPopup().regionGroup(parentRegion2).arrowButton().click();
        basePageSteps.onListingPage().geoPopup().region(region2).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText("Выбрано 2"));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", format("%s%%2C%s", regionCode1, regionCode2));
        urlSteps.testing().path(category).path(ALL).addParam("geo_id", regionCode1)
                .addParam("geo_id", regionCode2).shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().region().waitUntil(hasText(format("%s, %s", region1, region2)));
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(category).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button(format("%s + 1", region1)).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор России")
    public void shouldSelectRussia() {
        String region = "Россия";

        basePageSteps.onListingPage().paramsPopup().region().click();
        basePageSteps.onListingPage().geoPopup().region(region).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", RUSSIA_GEO_ID);
        basePageSteps.onListingPage().paramsPopup().radiusDisabled("+100 км").waitUntil(isDisplayed());
        urlSteps.testing().path(RUSSIA).path(category).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().region().waitUntil(hasText(region));
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button(region).waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс региона")
    public void shouldResetRegion() {
        String region = "Москва";
        String parentRegion = "Москва и Московская область";

        basePageSteps.onListingPage().paramsPopup().region().click();
        basePageSteps.onListingPage().geoPopup().regionGroup(parentRegion).arrowButton().click();
        basePageSteps.onListingPage().geoPopup().region(region).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText("Регион"));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", "");
        basePageSteps.onListingPage().paramsPopup().radiusDisabled("+100 км").waitUntil(isDisplayed());
        urlSteps.testing().path(category).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().region().waitUntil(hasText("Любой регион"));
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button("Любой регион").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс региона по кнопке «Сбросить»")
    public void shouldResetRegionByResetButton() {
        basePageSteps.onListingPage().paramsPopup().region().click();
        basePageSteps.onListingPage().geoPopup().resetButton().click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText("Регион"));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue("gids", "");
        basePageSteps.onListingPage().paramsPopup().radiusDisabled("+100 км").waitUntil(isDisplayed());
        urlSteps.testing().path(category).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().region().waitUntil(hasText("Любой регион"));
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().button("Любой регион").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор радиуса")
    public void shouldSelectRadius() {
        String radius = "100";

        basePageSteps.onListingPage().paramsPopup().radius(format("+%s км", radius)).click();
        basePageSteps.onListingPage().paramsPopup().radiusSelected(format("+%s км", radius)).waitUntil(isDisplayed());
        basePageSteps.onListingPage().paramsPopup().region().waitUntil(hasText(format("Москва + %s км", radius)));
        cookieSteps.shouldSeeCookieWithValue("gids", "213");
        cookieSteps.shouldSeeCookieWithValue("gradius", radius);
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().geoRadiusCounters().geoRadiusCounterActive(format("+ %s км", radius))
                .should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Закрытие поп-апа")
    public void shouldClosePopup() {
        basePageSteps.onListingPage().paramsPopup().region().click();
        basePageSteps.onListingPage().geoPopup().closeButton().click();
        basePageSteps.onListingPage().geoPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().paramsPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Закрытие саджеста регионов")
    public void shouldCloseRegionSuggest() {
        basePageSteps.onListingPage().paramsPopup().region().click();
        basePageSteps.onListingPage().geoPopup().searchRegionButton().click();
        basePageSteps.onListingPage().geoSuggestPopup().cancelButton().click();
        basePageSteps.onListingPage().geoSuggestPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().geoPopup().waitUntil(isDisplayed());
    }
}
