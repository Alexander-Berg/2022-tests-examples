package ru.auto.tests.forms.user;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;

@DisplayName("Частник, мотоциклы - сворачивание блока")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesFoldBlockTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        formsSteps.createMotorcyclesForm();
        urlSteps.testing().path(MOTO).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Сворачивание блока")
    @Category({Regression.class, Screenshooter.class})
    public void shouldFoldBlock() {
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getCategory().getBlock()).title().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        urlSteps.onCurrentUrl().setProduction().open();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getCategory().getBlock()).title().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}