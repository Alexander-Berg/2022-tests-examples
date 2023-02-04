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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SPECIAL;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Спецпредложения в каталоге")
@Feature(SPECIAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SpecialSalesCatalogTest {

    private static final int VISIBLE_ITEMS_CNT = 4;
    private String firstItemTitle;

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).open();
        firstItemTitle = basePageSteps.onBasePage().specialSales().getItem(0).title().getText();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение заголовка блока")
    public void shouldSeeSpecialSalesHeader() {
        screenshotSteps.setWindowSizeForScreenshot();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithCutting(basePageSteps.onBasePage().specialSales().title()
                        .should(isDisplayed()));

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithCutting(basePageSteps.onBasePage().specialSales().title()
                        .should(isDisplayed()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Поп-ап с описанием услуги")
    public void shouldSeePopup() {
        screenshotSteps.setWindowSizeForScreenshot();

        basePageSteps.onBasePage().specialSales().waitUntil(isDisplayed());
        basePageSteps.onBasePage().specialSales().how().waitUntil(isDisplayed()).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithCuttingIgnoreAreas(basePageSteps.onListingPage().activePopup()
                        .waitUntil(isDisplayed()), newHashSet(basePageSteps.onListingPage().activePopupImage()));

        urlSteps.setProduction().open();
        basePageSteps.onBasePage().specialSales().waitUntil(isDisplayed());
        basePageSteps.onBasePage().specialSales().how().waitUntil(isDisplayed()).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithCuttingIgnoreAreas(basePageSteps.onListingPage().activePopup()
                        .waitUntil(isDisplayed()), newHashSet(basePageSteps.onListingPage().activePopupImage()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Ссылка в поп-апе")
    public void shouldClickPopupUrl() {
        basePageSteps.onBasePage().specialSales().waitUntil(isDisplayed());
        basePageSteps.onBasePage().specialSales().how().waitUntil(isDisplayed()).hover();
        basePageSteps.onBasePage().activePopupLink().click();
        urlSteps.testing().path(MY).path(CARS).addParam("from", "specials_block")
                .addParam("vas_service", "special").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Листание объявлений")
    public void shouldSlideSales() {
        basePageSteps.onBasePage().specialSales().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onBasePage().specialSales().itemsList().get(VISIBLE_ITEMS_CNT)
                .should(not(isDisplayed()));
        basePageSteps.onBasePage().specialSales().nextButton().waitUntil(isDisplayed()).click();
        basePageSteps.onBasePage().specialSales().itemsList().get(VISIBLE_ITEMS_CNT)
                .should(isDisplayed());
        basePageSteps.onBasePage().specialSales().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onBasePage().specialSales().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onBasePage().specialSales().itemsList().get(VISIBLE_ITEMS_CNT)
                .should(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по фото объявления")
    public void shouldClickSalePhoto() {
        basePageSteps.onBasePage().specialSales().getItem(0).image().should(isDisplayed()).click();
        basePageSteps.onCardPage().cardHeader().title().should(hasText(containsString(firstItemTitle)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по названию объявления")
    public void shouldClickSaleTitle() {
        basePageSteps.onBasePage().specialSales().getItem(0).title().should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        basePageSteps.onCardPage().cardHeader().title().should(hasText(containsString(firstItemTitle)));
    }
}