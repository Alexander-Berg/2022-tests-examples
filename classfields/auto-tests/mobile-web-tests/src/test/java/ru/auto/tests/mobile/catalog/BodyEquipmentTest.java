package ru.auto.tests.mobile.catalog;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import pazone.ashot.Screenshot;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static com.google.common.collect.Sets.newHashSet;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.EQUIPMENT;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Каталог - карточка кузова - комплектации")
@Feature(AutoruFeatures.CATALOG)
public class BodyEquipmentTest {

    private static final String MARK = "audi";
    private static final String MODEL = "q3";
    private static final String GENERATION_ID = "21356775";
    private static final String BODY_ID = "21356854";
    private static final String OTHER_COMPLECTATION_ID = "21356854__21447968/";
    private static final String OTHER_COMPLECTATION = "1.5 MT 150 л.c. ";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID)
                .path(EQUIPMENT).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Ignore // Баг VERTISTEST-2236
    @DisplayName("Выбор опций комплектации")
    @Category({Testing.class, Screenshooter.class})
    public void shouldSelectComplectationOptions() {
        basePageSteps.setWindowMaxHeight();

        selectComplectationOptions();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotIgnoreAreas(basePageSteps.onCatalogBodyPage().complectationDescription()
                        .waitUntil(isDisplayed()), newHashSet(basePageSteps.onCatalogBodyPage().c2advert()));

        urlSteps.onCurrentUrl().setProduction().open();
        selectComplectationOptions();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotIgnoreAreas(basePageSteps.onCatalogBodyPage().complectationDescription()
                        .waitUntil(isDisplayed()), newHashSet(basePageSteps.onCatalogBodyPage().c2advert()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Просмотр другой комплектации")
    public void shouldSeeOtherComplectation() {
        basePageSteps.onCatalogBodyPage().selector("Выбрать комплектацию").should(isDisplayed()).click();
        basePageSteps.onCatalogBodyPage().dropdown().item(OTHER_COMPLECTATION).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID)
                .path(SPECIFICATIONS).path(OTHER_COMPLECTATION_ID).path("/").ignoreParam("cookiesync")
                .shouldNotSeeDiff();
    }

    @Step("Выбираем опции комплектации")
    private void selectComplectationOptions() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCatalogBodyPage().option("Круиз-контроль"), 0, 100);
        basePageSteps.onCatalogBodyPage().option("Круиз-контроль").hover().click();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCatalogBodyPage().optionPackage("Advance"), 0, 100);
        basePageSteps.onCatalogBodyPage().optionPackage("Advance").click();
    }
}
