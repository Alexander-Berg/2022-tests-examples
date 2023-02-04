package ru.auto.tests.poffer.dealer;

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
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;

@DisplayName("Дилер - блок марки")
@Feature(POFFER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MarkBlockTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String section;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Used"},
                {"New"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "desktop/SearchCarsBreadcrumbsRid213",
                "desktop/Currencies",
                "desktop/ReferenceCatalogCarsAllOptions",
                "poffer/ReferenceCatalogCarsSuggestLifanSolano",
                format("poffer/dealer/UserDraftCars%sEmpty", section),
                format("poffer/dealer/UserDraftCarsDraftIdGet%sEmpty", section),
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(CARS).path(section.toLowerCase()).path(ADD).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение формы")
    @Category({Regression.class, Screenshooter.class, Testing.class})
    public void shouldSeeForm() {
        pofferSteps.setWindowHeight(8000);

        pofferSteps.onPofferPage().firstStepStsVinBlock().button("Пропустить").click();
        pofferSteps.fillMark("Lifan");
        pofferSteps.onPofferPage().markLogo().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(pofferSteps.onPofferPage().form());

        urlSteps.onCurrentUrl().setProduction().open();
        pofferSteps.onPofferPage().firstStepStsVinBlock().button("Пропустить").click();
        pofferSteps.fillMark("Lifan");
        pofferSteps.onPofferPage().markLogo().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(pofferSteps.onPofferPage().form());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
