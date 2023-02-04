package ru.auto.tests.mobile.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.SPB_GEO_ID;
import static ru.auto.tests.desktop.step.CookieSteps.GIDS;
import static ru.auto.tests.desktop.step.CookieSteps.GRADIUS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Регион")
@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class GeoRegionTest {

    private static final String MOSCOW = "Москва";
    private static final String REGION = "Санкт-Петербург";
    private static final String PARENT_REGION = "Санкт-Петербург и Ленинградская область";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор региона")
    public void shouldSelectRegion() {
        urlSteps.testing().path(SLASH).open();
        basePageSteps.onMainPage().filters().button(MOSCOW).click();
        basePageSteps.onMainPage().geoPopup().waitUntil(isDisplayed());
        basePageSteps.onMainPage().geoPopup().resetButton().click();
        basePageSteps.onMainPage().geoPopup().regionGroup(PARENT_REGION).arrowButton().click();
        basePageSteps.onMainPage().geoPopup().region(REGION).click();
        basePageSteps.onMainPage().geoPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onMainPage().geoPopup().readyButton().click();
        urlSteps.shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, SPB_GEO_ID);
        cookieSteps.shouldNotSeeCookie(GRADIUS);
        basePageSteps.onMainPage().filters().button(REGION).waitUntil(isDisplayed());
        basePageSteps.onMainPage().filters().button("+ 200 км").waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс региона")
    public void shouldResetRegion() {
        urlSteps.testing().addParam(GEO_ID, SPB_GEO_ID).open();
        basePageSteps.onMainPage().filters().button(REGION).resetButton().click();

        urlSteps.shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, "");
        cookieSteps.shouldNotSeeCookie(GRADIUS);
        basePageSteps.onMainPage().filters().button("Любой регион").waitUntil(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class})
    @DisplayName("Выбор двух регионов")
    public void shouldSelectTwoRegions() {
        String secondRegion = "Бокситогорск";
        String secondRegionId = "10861";

        urlSteps.testing().open();
        basePageSteps.onMainPage().filters().button(MOSCOW).click();
        basePageSteps.onMainPage().geoPopup().waitUntil(isDisplayed());
        basePageSteps.onMainPage().geoPopup().resetButton().click();
        basePageSteps.onMainPage().geoPopup().regionGroup(PARENT_REGION).arrowButton().click();
        basePageSteps.onMainPage().geoPopup().region(REGION).click();
        basePageSteps.onMainPage().geoPopup().region(secondRegion).click();
        basePageSteps.onMainPage().geoPopup().title().waitUntil(hasText("Выбрано 2"));
        basePageSteps.onMainPage().geoPopup().readyButton().click();

        cookieSteps.shouldSeeCookieWithValue(GIDS, format("%s%%2C%s", SPB_GEO_ID, secondRegionId));
        basePageSteps.onMainPage().filters().button(format("%s + 1", REGION)).waitUntil(isDisplayed());
        urlSteps.path(SLASH).shouldNotSeeDiff();
    }

}
