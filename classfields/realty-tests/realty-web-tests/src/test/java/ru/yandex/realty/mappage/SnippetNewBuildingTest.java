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
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;
import static ru.yandex.realty.element.map.Sidebar.BACK_TO_FILTERS;
import static ru.yandex.realty.element.map.Sidebar.CALL_ME;

@DisplayName("Карта. Общее. Сниппет новостройки")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SnippetNewBuildingTest {

    private static final int MAP_PIN = 6;

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
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.onMapPage().filters().waitUntil(isDisplayed());
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(MAP_PIN));
        basePageSteps.onMapPage().filters().waitUntil(not(isDisplayed()));
        basePageSteps.onMapPage().sidebar().newbuildingCard().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход с сайдбара обратно к фильтрам")
    public void shouldSeeBackToFilters() {
        basePageSteps.onMapPage().sidebar().spanLink(BACK_TO_FILTERS).click();
        basePageSteps.onMapPage().sidebar().spanLink(BACK_TO_FILTERS).waitUntil(not(isDisplayed()));
        basePageSteps.onMapPage().filters().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход на карточку ЖК")
    public void shouldSeePassToNewbuildingCard() {
        String href = basePageSteps.onMapPage().sidebar().newbuildingCard().jkLink().getAttribute("href");
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().sidebar().newbuildingCard().jkLink());
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(href).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход на карточку застройщика")
    public void shouldSeePassToDeveloperCard() {
        String href = basePageSteps.onMapPage().sidebar().newbuildingCard().developerLink().getAttribute("href");
        basePageSteps.onMapPage().sidebar().newbuildingCard().developerLink().click();
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(href).shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Показать телефон» -> видим рекламку")
    public void shouldSeeShowPhoneButton() {
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().click();
        basePageSteps.onMapPage().adPopup().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот попапа рекламы при нажатии на «Показать телефон»")
    public void shouldSeeShowPhoneScreenshotPopup() {
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().adPopup());

        urlSteps.setProductionHost().open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(MAP_PIN));
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().adPopup());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот попапа при нажатии на «Позвоните мне»")
    public void shouldSeeCallMeScreenshotPopup() {
        basePageSteps.onMapPage().sidebar().newbuildingCard().button(CALL_ME).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().callBackPopup());

        urlSteps.setProductionHost().open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(MAP_PIN));
        basePageSteps.onMapPage().sidebar().newbuildingCard().button(CALL_ME).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().callBackPopup());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем «Добавить в избранное»")
    public void shouldSeeAddToFavorite() {
        basePageSteps.onMapPage().sidebar().newbuildingCard().addToFavorite().click();
        basePageSteps.onMapPage().sidebar().newbuildingCard().addToFavorite()
                .should(hasClass(containsString("_active")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот попапа подписок при клике «Добавить в избранное»")
    public void shouldSeeSubscriptionPopupScreenshot() {
        basePageSteps.onMapPage().sidebar().newbuildingCard().addToFavorite().click();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().subscriptionPopupNb());

        urlSteps.setProductionHost().open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(MAP_PIN));
        basePageSteps.onMapPage().sidebar().newbuildingCard().addToFavorite()
                .waitUntil(hasClass(containsString("_active")));
        basePageSteps.onMapPage().sidebar().newbuildingCard().addToFavorite().click();
        basePageSteps.onMapPage().sidebar().newbuildingCard().addToFavorite()
                .waitUntil(hasClass(not(containsString("_active"))));
        basePageSteps.onMapPage().sidebar().newbuildingCard().addToFavorite().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().subscriptionPopupNb());
        compareSteps.screenshotsShouldBeTheSame(testing, production);

    }
}
