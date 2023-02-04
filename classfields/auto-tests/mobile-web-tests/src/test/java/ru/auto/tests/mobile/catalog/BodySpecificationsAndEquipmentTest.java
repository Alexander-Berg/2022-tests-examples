package ru.auto.tests.mobile.catalog;

import com.carlosbecker.guice.GuiceModules;
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
import pazone.ashot.Screenshot;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;

@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Каталог - карточка модели - характеристики, комплектации")
@Feature(AutoruFeatures.CATALOG)
public class BodySpecificationsAndEquipmentTest {

    private static final String MARK = "audi";
    private static final String MODEL = "a5";
    private static final String GENERATION_ID = "20795592";
    private static final String BODY_ID = "20795627";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String path;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/specifications/"},
                {"/equipment/"}
        });
    }

    @Before
    public void before() {
        basePageSteps.setWindowMaxHeight();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID).path(path)
                .open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Описание модификации/комплектации")
    public void shouldSeeDescription() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotIgnoreAreas(basePageSteps.onCatalogBodyPage().complectationDescription(),
                        newHashSet(basePageSteps.onCatalogBodyPage().c2advert()));

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotIgnoreAreas(basePageSteps.onCatalogBodyPage().complectationDescription(),
                        newHashSet(basePageSteps.onCatalogBodyPage().c2advert()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
