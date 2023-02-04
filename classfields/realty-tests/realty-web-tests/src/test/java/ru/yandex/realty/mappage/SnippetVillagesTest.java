package ru.yandex.realty.mappage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;

@DisplayName("Карта. Общее. Сниппет котеджного поселка")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SnippetVillagesTest {

    private static final int MAP_PIN = 5;
    private static final String PARAMETERS_TAB = "Параметры";
    private static final String ACTIVE_CLASS_PARAM = "_active";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.onMapPage().filters().waitUntil(isDisplayed());
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(MAP_PIN));
        basePageSteps.onMapPage().filters().waitUntil(not(isDisplayed()));
        basePageSteps.onMapPage().sidebar().villageMapOffer().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход с карточки обратно к фильтрам")
    public void shouldSeeBackToFilters() {
        basePageSteps.onMapPage().sidebar().spanLink(PARAMETERS_TAB).click();
        basePageSteps.onMapPage().sidebar().spanLink(PARAMETERS_TAB).waitUntil(hasClass(containsString(ACTIVE_CLASS_PARAM)));
        basePageSteps.onMapPage().filters().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход на карточку КП")
    public void shouldSeePassToNewbuildingCard() {
        String href = basePageSteps.onMapPage().sidebar().villageMapOffer().kpLink().getAttribute("href");
        basePageSteps.onMapPage().sidebar().villageMapOffer().click();
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(href).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход на карточку застройщика")
    public void shouldSeePassToDeveloperCard() {
        basePageSteps.onMapPage().sidebar().villageMapOffer().developerLink().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KOTTEDZHNYE_POSELKI)
                .queryParam("developerId", "2323");
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Показать телефон» -> видим рекламку")
    public void shouldSeeShowPhoneButton() {
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton().click();
        basePageSteps.onMapPage().adPopup().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот попапа рекламы при нажатии на «Показать телефон»")
    public void shouldSeeShowPhoneScreenshotPopup() {
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().adPopup());

        urlSteps.setProductionHost().open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(MAP_PIN));
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().adPopup());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем «Добавить в избранное»")
    public void shouldSeeAddToFavorite() {
        basePageSteps.onMapPage().sidebar().villageMapOffer().addToFavorite().click();
        basePageSteps.onMapPage().sidebar().villageMapOffer().addToFavorite()
                .should(hasClass(containsString(ACTIVE_CLASS_PARAM)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Убираем из избранного")
    public void shouldSeeRemoveFromFavorite() {
        basePageSteps.onMapPage().sidebar().villageMapOffer().addToFavorite().click();
        basePageSteps.onMapPage().sidebar().villageMapOffer().addToFavorite()
                .waitUntil(hasClass(containsString(ACTIVE_CLASS_PARAM)));
        basePageSteps.onMapPage().sidebar().villageMapOffer().addToFavorite().click();
        basePageSteps.onMapPage().sidebar().villageMapOffer().addToFavorite()
                .waitUntil(hasClass(not(containsString(ACTIVE_CLASS_PARAM))));
    }
}
