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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - карточка марки")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MarkCardTest {

    private static final String MARK = "Audi";
    private static final Set<String> IGNORE_ELEMENTS = newHashSet("//div[contains(@class, 'mosaic__image')]",
            "//div[@class='mosaic__p']");
    private static final int ALL_MODELS_LIST_SIZE = 34;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path("/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели в списке популярных моделей")
    public void shouldSelectPopularModel() throws IOException {
        catalogPageSteps.onCatalogPage().filter().markModelGenBlock().breadcrumbsItem("Выбрать модель ")
                .should(isDisplayed()).click();
        String model = catalogPageSteps.selectFirstModel();
        urlSteps.path(model.toLowerCase()).path("/").ignoreParam("view_type").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели в списке всех моделей")
    public void shouldSelectUnpopularModel() throws IOException {
        catalogPageSteps.onCatalogPage().filter().markModelGenBlock().breadcrumbsItem("Выбрать модель ")
                .should(isDisplayed()).click();
        catalogPageSteps.onCatalogPage().filter().markModelGenBlock().switcher("Все").waitUntil(isDisplayed())
                .click();
        String model = catalogPageSteps.selectFirstModel();
        urlSteps.path(model.toLowerCase()).path("/").ignoreParam("view_type").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Описание")
    public void shouldSeeDescription() {
        catalogPageSteps.setNarrowWindowSize();

        catalogPageSteps.onCatalogPage().markLogo().hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().markInfo());

        urlSteps.setProduction().open();
        catalogPageSteps.onCatalogPage().markLogo().hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().markInfo());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Подробнее")
    public void shouldSeeMoreInfo() {
        catalogPageSteps.setNarrowWindowSize();

        catalogPageSteps.onCatalogPage().moreInfoButton().should(isDisplayed()).click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().markInfo());

        urlSteps.setProduction().open();
        catalogPageSteps.onCatalogPage().moreInfoButton().should(isDisplayed()).click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().markInfo());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Популярные модели")
    public void shouldSeePopularModels() {
        catalogPageSteps.setNarrowWindowSize();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(catalogPageSteps.onCatalogMarkPage().popularModels()
                        .waitUntil(isDisplayed()), IGNORE_ELEMENTS);

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(catalogPageSteps.onCatalogMarkPage().popularModels()
                        .waitUntil(isDisplayed()), IGNORE_ELEMENTS);

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Все модели")
    public void shouldSeeAllModels() {
        catalogPageSteps.onCatalogMarkPage().allModelsListInTableViewType().should(hasSize(ALL_MODELS_LIST_SIZE))
                .forEach(model -> model.should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Популярные модели списком")
    public void shouldSeePopularModelsAsList() {
        catalogPageSteps.setNarrowWindowSize();

        urlSteps.addParam("view_type", "list").open();
        catalogPageSteps.onCatalogMarkPage().popularAllSwitcher("Популярные").should(isDisplayed()).click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogMarkPage().modelsInListViewType());

        urlSteps.setProduction().open();
        catalogPageSteps.onCatalogMarkPage().popularAllSwitcher("Популярные").should(isDisplayed()).click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogMarkPage().modelsInListViewType());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Все модели списком")
    public void shouldSeeAllModelsAsList() {
        catalogPageSteps.setNarrowWindowSize();

        urlSteps.addParam("view_type", "list").open();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogMarkPage().modelsInListViewType());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogMarkPage().modelsInListViewType());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по популярной модели в табличном виде")
    public void shouldClickPopularModelInTableViewType() {
        catalogPageSteps.onCatalogMarkPage().getModel(0).click();
        catalogPageSteps.onCatalogPage().modelSummary().waitUntil(isDisplayed());
    }
}
