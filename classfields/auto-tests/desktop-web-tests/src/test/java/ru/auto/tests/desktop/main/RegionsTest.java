package ru.auto.tests.desktop.main;

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
import ru.auto.tests.desktop.consts.Pages;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.KOPITSA;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Regions.KRASNOYARSK;
import static ru.auto.tests.desktop.consts.Regions.KRASNOYARSK_GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.KRASNOYARSK_IP;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.consts.Regions.RUSSIA;
import static ru.auto.tests.desktop.consts.Regions.RUSSIA_GEO_ID;
import static ru.auto.tests.desktop.consts.Regions.SPB;
import static ru.auto.tests.desktop.consts.Regions.SPB_GEO_ID;
import static ru.auto.tests.desktop.step.CookieSteps.GIDS;
import static ru.auto.tests.desktop.step.CookieSteps.GRADIUS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - шапка - геоселектор")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class RegionsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    private static final String RADIUS = "500";

    @Before
    public void before() {
        cookieSteps.deleteCookie(GIDS);
        urlSteps.testing().addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Отображение поп-апа геоселекта")
    public void shouldOpenGeoSelectPopup() {
        basePageSteps.onMainPage().subHeader().geoSelectButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().should(hasText("Регион, город, населенный пункт\nМосква\n" +
                "Расширение радиуса поиска, км\n0\n100\n200\n300\n400\n500\n1000\nСохранить"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Изменяем радиус")
    public void shouldSeeRadiusCookie() {
        basePageSteps.onMainPage().subHeader().geoSelectButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().radiusButton(RADIUS).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().should(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(GRADIUS, RADIUS);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор региона из списка дефолтных")
    public void shouldSelectRegionFromDefaults() {
        basePageSteps.onMainPage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(MOSCOW).should(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().defaultRegion(SPB).click();
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(SPB).waitUntil(isDisplayed());
        basePageSteps.onMainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(Pages.SPB).addXRealIP(MOSCOW_IP).shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, SPB_GEO_ID);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор региона из списка дефолтных, определенных по IP (Красноярск)")
    public void shouldSelectRegionFromIpDefaults() {
        urlSteps.testing().path(MOSKVA).addXRealIP(KRASNOYARSK_IP).open();
        basePageSteps.onMainPage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(MOSCOW).click();
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(MOSCOW).waitUntil(not(isDisplayed()));
        basePageSteps.onMainPage().geoSelectPopup().defaultRegion(KRASNOYARSK).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(KRASNOYARSK).waitUntil(isDisplayed());
        basePageSteps.onMainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(Pages.KRASNOYARSK).path(SLASH).addXRealIP(KRASNOYARSK_IP).shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, KRASNOYARSK_GEO_ID);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор региона из саджеста")
    public void shouldSelectRegionFromSuggest() {
        basePageSteps.onMainPage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(MOSCOW).click();
        basePageSteps.onMainPage().geoSelectPopup().regionInput().sendKeys(SPB);
        basePageSteps.onMainPage().geoSelectPopup().suggestItem(SPB).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(Pages.SPB).addXRealIP(MOSCOW_IP).shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, SPB_GEO_ID);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор России из саджеста")
    public void shouldSelectRussiaFromSuggest() {
        basePageSteps.onMainPage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(MOSCOW).click();
        basePageSteps.onMainPage().geoSelectPopup().regionInput().sendKeys(RUSSIA);
        basePageSteps.onMainPage().geoSelectPopup().suggestItem(RUSSIA).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(Pages.RUSSIA).addXRealIP(MOSCOW_IP).shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, RUSSIA_GEO_ID);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление региона")
    public void shouldRemoveRegion() {
        basePageSteps.onMainPage().subHeader().geoSelectButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().waitUntil(isDisplayed());
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(MOSCOW).click();
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(MOSCOW).waitUntil(not(isDisplayed()));
        basePageSteps.onMainPage().geoSelectPopup().defaultRegion(MOSCOW).waitUntil(isDisplayed());
        basePageSteps.onMainPage().geoSelectPopup().defaultRegion(SPB).waitUntil(isDisplayed());
        basePageSteps.onMainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().addXRealIP(MOSCOW_IP).shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, "");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбор второго региона")
    public void shouldSelectSecondRegion() {
        basePageSteps.onMainPage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().regionInput().sendKeys(SPB);
        basePageSteps.onMainPage().geoSelectPopup().suggestItem(SPB).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().waitUntil(not(isDisplayed()));

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

        basePageSteps.onMainPage().subHeader().geoSelectButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().selectedRegion(MOSCOW).click();
        basePageSteps.onMainPage().geoSelectPopup().regionInput().sendKeys(SPB);
        basePageSteps.onMainPage().geoSelectPopup().suggestItem(SPB).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().regionInput().sendKeys(secondRegion);
        basePageSteps.onMainPage().geoSelectPopup().suggestItem(secondRegion).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().confirmButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().geoSelectPopup().waitUntil(not(isDisplayed()));

        urlSteps.shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue(GIDS, format("%s%%2C%s", SPB_GEO_ID, secondRegionId));
    }

}
