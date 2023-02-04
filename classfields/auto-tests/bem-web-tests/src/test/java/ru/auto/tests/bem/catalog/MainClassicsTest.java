package ru.auto.tests.bem.catalog;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - главная - классика")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MainClassicsTest {

    private static final Integer MAX_ITEMS_CNT = 28;
    private static final Integer VISIBLE_ITEMS_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).open();
        screenshotSteps.setWindowSizeForScreenshot();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение заголовка")
    public void shouldSeeTitle() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().classics().title());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().classics().title());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Смотреть все»")
    public void shouldClickShowAllButton() {
        catalogPageSteps.onCatalogPage().classics().showAll().waitUntil(isDisplayed()).click();
        urlSteps.path(CARS).path(ALL).addParam("year_to", "1980").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по фото модели")
    public void shouldClickModelPhoto() {
        catalogPageSteps.onCatalogPage().classics().modelsList().get(0).imageUrl().waitUntil(isDisplayed()).click();
        catalogPageSteps.onCatalogPage().title()
                .should(hasAttribute("textContent",
                        containsString("технические характеристики и\u00a0комплектации")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по названию модели")
    public void shouldClickModelTitle() {
        catalogPageSteps.onCatalogPage().classics().modelsList().get(0).titleUrl().waitUntil(isDisplayed()).click();
        catalogPageSteps.onCatalogPage().title()
                .should(hasAttribute("textContent",
                        containsString("технические характеристики и\u00a0комплектации")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Листание моделей")
    public void shouldSlideModels() {
        catalogPageSteps.onCatalogPage().classics().modelsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(e -> isDisplayed());
        catalogPageSteps.onCatalogPage().classics().modelsList().get(VISIBLE_ITEMS_CNT + 1).should(not(isDisplayed()));
        catalogPageSteps.onCatalogPage().classics().nextButton().waitUntil(isDisplayed()).click();
        catalogPageSteps.onCatalogPage().classics().modelsList().get(VISIBLE_ITEMS_CNT + 1).should(isDisplayed());
        catalogPageSteps.onCatalogPage().classics().prevButton().waitUntil(isDisplayed()).click();
        catalogPageSteps.onCatalogPage().classics().modelsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(e -> isDisplayed());
        catalogPageSteps.onCatalogPage().classics().modelsList().get(VISIBLE_ITEMS_CNT + 1).should(not(isDisplayed()));
    }
}