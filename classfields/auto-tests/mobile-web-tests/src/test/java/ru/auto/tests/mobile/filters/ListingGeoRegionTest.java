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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SPB;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.QueryParams.GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.SPB_GEO_ID;
import static ru.auto.tests.desktop.step.CookieSteps.GIDS;
import static ru.auto.tests.desktop.step.CookieSteps.GRADIUS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Регион")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingGeoRegionTest {

    private static final String MOSCOW = "Москва";
    private static final String REGION = "Санкт-Петербург";
    private static final String PARENT_REGION = "Санкт-Петербург и Ленинградская область";
    private static final String CHOSED_ONE = "Выбрано 1";

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

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[]{
                CARS,
                TRUCK,
                MOTORCYCLE
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск региона")
    public void shouldSearchRegion() {
        basePageSteps.onListingPage().filters().button(MOSCOW).click();
        basePageSteps.onListingPage().geoPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().geoPopup().resetButton().click();
        basePageSteps.onListingPage().geoPopup().searchRegionButton().click();
        basePageSteps.onListingPage().geoSuggestPopup().input("Населённый пункт", REGION);
        basePageSteps.onListingPage().geoSuggestPopup().regionsList().waitUntil(hasSize(2));
        basePageSteps.onListingPage().geoSuggestPopup().getRegion(1)
                .should(hasText(format("%s\n%s", REGION, PARENT_REGION))).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText(CHOSED_ONE));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue(GIDS, SPB_GEO_ID);
        basePageSteps.onListingPage().filters().button(REGION).waitUntil(isDisplayed());
        urlSteps.testing().path(SPB).path(category).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор региона")
    public void shouldSelectRegion() {
        basePageSteps.onListingPage().filters().button(MOSCOW).click();
        basePageSteps.onListingPage().geoPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().geoPopup().resetButton().click();
        basePageSteps.onListingPage().geoPopup().regionGroup(PARENT_REGION)
                .arrowButton().click();
        basePageSteps.onListingPage().geoPopup().region(REGION).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText(CHOSED_ONE));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        cookieSteps.shouldSeeCookieWithValue(GIDS, SPB_GEO_ID);
        basePageSteps.onListingPage().filters().button(REGION).waitUntil(isDisplayed());
        urlSteps.testing().path(SPB).path(category).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class})
    @DisplayName("Выбор двух регионов с общим родительским регионом")
    public void shouldSelectTwoRegions() {
        String secondRegion = "Бокситогорск";
        String secondRegionId = "10861";

        basePageSteps.onListingPage().filters().button(MOSCOW).click();
        basePageSteps.onListingPage().geoPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().geoPopup().resetButton().click();
        basePageSteps.onListingPage().geoPopup().regionGroup(PARENT_REGION)
                .arrowButton().click();
        basePageSteps.onListingPage().geoPopup().region(REGION).click();
        basePageSteps.onListingPage().geoPopup().region(secondRegion).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText("Выбрано 2"));
        basePageSteps.onListingPage().geoPopup().readyButton().click();

        cookieSteps.shouldSeeCookieWithValue(GIDS, format("%s%%2C%s", SPB_GEO_ID, secondRegionId));
        basePageSteps.onListingPage().filters().button(format("%s + 1", REGION)).waitUntil(isDisplayed());
        urlSteps.testing().path(category).path(ALL).addParam(GEO_ID, SPB_GEO_ID).addParam(GEO_ID, secondRegionId)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class})
    @DisplayName("Выбор двух регионов с разным родительским регионом")
    public void shouldSelectTwoRegionsDifferentParent() {
        String secondRegion = "Барнаул";
        String secondRegionParent = "Алтайский край";
        String secondRegionId = "197";

        basePageSteps.onListingPage().filters().button(MOSCOW).click();
        basePageSteps.onListingPage().geoPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().geoPopup().resetButton().click();
        basePageSteps.onListingPage().geoPopup().regionGroup(PARENT_REGION).arrowButton().click();
        basePageSteps.onListingPage().geoPopup().region(REGION).click();
        basePageSteps.onListingPage().geoPopup().regionGroup(PARENT_REGION).arrowButton().click();
        basePageSteps.onListingPage().geoPopup().regionGroup(secondRegionParent).arrowButton().click();
        basePageSteps.onListingPage().geoPopup().region(secondRegion).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText("Выбрано 2"));
        basePageSteps.onListingPage().geoPopup().readyButton().click();

        cookieSteps.shouldSeeCookieWithValue(GIDS, format("%s%%2C%s", SPB_GEO_ID, secondRegionId));
        basePageSteps.onListingPage().filters().button(format("%s + 1", REGION)).waitUntil(isDisplayed());
        urlSteps.testing().path(category).path(ALL).addParam(GEO_ID, SPB_GEO_ID).addParam(GEO_ID, secondRegionId)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Обновление листинга после выбора региона")
    public void shouldResetListingAfterRegionSelect() {
        String salesCount = basePageSteps.onListingPage().sortBar().offersCount().getText();
        String firstSaleText = basePageSteps.onListingPage().getSale(0).getText();

        basePageSteps.onListingPage().filters().button(MOSCOW).click();
        basePageSteps.onListingPage().geoPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().geoPopup().resetButton().click();
        basePageSteps.onListingPage().geoPopup().regionGroup(PARENT_REGION)
                .arrowButton().click();
        basePageSteps.onListingPage().geoPopup().region(REGION).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText(CHOSED_ONE));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        basePageSteps.onListingPage().sortBar().offersCount().waitUntil(not(hasText(salesCount)));
        basePageSteps.onListingPage().getSale(0).title().waitUntil(not(hasText(firstSaleText)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Обновление ссылки в шапке после выбора региона")
    public void shouldResetLogoUrlAfterRegionSelect() {
        basePageSteps.onListingPage().filters().button(MOSCOW).click();
        basePageSteps.onListingPage().geoPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().geoPopup().resetButton().click();
        basePageSteps.onListingPage().geoPopup().regionGroup(PARENT_REGION)
                .arrowButton().click();
        basePageSteps.onListingPage().geoPopup().region(REGION).click();
        basePageSteps.onListingPage().geoPopup().title().waitUntil(hasText(CHOSED_ONE));
        basePageSteps.onListingPage().geoPopup().readyButton().click();
        basePageSteps.onListingPage().header().logo().click();
        urlSteps.testing().path(SPB).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс региона")
    public void shouldResetRegion() {
        basePageSteps.onListingPage().filters().button(MOSCOW).resetButton().click();
        urlSteps.testing().path(category).path(ALL).shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, "");
        cookieSteps.shouldNotSeeCookie(GRADIUS);
        basePageSteps.onListingPage().filters().button("Любой регион").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Обновление листинга после сброса региона")
    public void shouldResetListingAfterRegionReset() throws InterruptedException {
        String salesCount = basePageSteps.onListingPage().sortBar().offersCount().getText();
        String firstSaleText = basePageSteps.onListingPage().getSale(0).getText();
        basePageSteps.onListingPage().filters().button(MOSCOW).resetButton().click();
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.onListingPage().sortBar().offersCount().waitUntil(not(hasText(salesCount)));
        basePageSteps.onListingPage().getSale(0).title().waitUntil(not(hasText(firstSaleText)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Обновление ссылки в шапке после сброса региона")
    public void shouldResetLogoUrlAfterRegionReset() {
        basePageSteps.onListingPage().filters().button(MOSCOW).resetButton().click();
        basePageSteps.onListingPage().header().logo().click();
        urlSteps.testing().path("/").shouldNotSeeDiff();
    }
}
