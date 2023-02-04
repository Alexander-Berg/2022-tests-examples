package ru.auto.tests.desktopcompare.header;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_OFFERS;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.consts.Regions.SPB;
import static ru.auto.tests.desktop.consts.Regions.SPB_GEO_ID;
import static ru.auto.tests.desktop.step.CookieSteps.GIDS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Шапка - регион")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class RegionTest {

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
        cookieSteps.deleteCookie(GIDS);
        urlSteps.testing().path(COMPARE_OFFERS).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор региона из списка дефолтных")
    public void shouldSelectRegionFromDefaults() {
        basePageSteps.onComparePage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().selectedRegion(MOSCOW).should(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().defaultRegion(SPB).click();
        basePageSteps.onComparePage().geoSelectPopup().selectedRegion(SPB).waitUntil(isDisplayed());
        basePageSteps.onComparePage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().waitUntil(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onComparePage().subHeader().geoSelectButton().should(hasText("Санкт-Петербург + 200 км"));
        cookieSteps.shouldSeeCookieWithValue(GIDS, SPB_GEO_ID);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор региона из саджеста")
    public void shouldSelectRegionFromSuggest() {
        basePageSteps.onComparePage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().selectedRegion(MOSCOW).click();
        basePageSteps.onComparePage().geoSelectPopup().regionInput().sendKeys(SPB);
        basePageSteps.onComparePage().geoSelectPopup().suggestItem(SPB).waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().waitUntil(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onComparePage().subHeader().geoSelectButton().should(hasText("Санкт-Петербург + 200 км"));
        cookieSteps.shouldSeeCookieWithValue(GIDS, SPB_GEO_ID);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление региона")
    public void shouldRemoveRegion() {
        basePageSteps.onComparePage().subHeader().geoSelectButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().waitUntil(isDisplayed());
        basePageSteps.onComparePage().geoSelectPopup().selectedRegion(MOSCOW).click();
        basePageSteps.onComparePage().geoSelectPopup().selectedRegion(MOSCOW).waitUntil(not(isDisplayed()));
        basePageSteps.onComparePage().geoSelectPopup().defaultRegion(MOSCOW).waitUntil(isDisplayed());
        basePageSteps.onComparePage().geoSelectPopup().defaultRegion(SPB).waitUntil(isDisplayed());
        basePageSteps.onComparePage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().waitUntil(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onComparePage().subHeader().geoSelectButton().should(hasText("Любой регион"));
        cookieSteps.shouldSeeCookieWithValue(GIDS, "");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбор второго региона")
    public void shouldSelectSecondRegion() {
        basePageSteps.onComparePage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().regionInput().sendKeys(SPB);
        basePageSteps.onComparePage().geoSelectPopup().suggestItem(SPB).waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().waitUntil(not(isDisplayed()));

        urlSteps.shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, format("%s%%2C%s", MOSCOW_GEO_ID, SPB_GEO_ID));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбор двух регионов из саджеста ")
    public void shouldSelectTwoRegionsFromSuggest() {
        String secondRegion = "Барнаул";
        String secondRegionId = "197";

        basePageSteps.onComparePage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().selectedRegion(MOSCOW).click();
        basePageSteps.onComparePage().geoSelectPopup().regionInput().sendKeys(SPB);
        basePageSteps.onComparePage().geoSelectPopup().suggestItem(SPB).waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().regionInput().sendKeys(secondRegion);
        basePageSteps.onComparePage().geoSelectPopup().suggestItem(secondRegion).waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().geoSelectPopup().waitUntil(not(isDisplayed()));

        urlSteps.shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, format("%s%%2C%s", SPB_GEO_ID, secondRegionId));
    }

}
