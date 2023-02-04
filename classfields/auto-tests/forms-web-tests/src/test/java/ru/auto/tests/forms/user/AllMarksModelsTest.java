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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.io.IOException;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;

@DisplayName("Частник, мотоциклы - все марки/все модели")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AllMarksModelsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        formsSteps.createMotorcyclesForm();
        formsSteps.getMark().setValue("Honda");
        formsSteps.setWindowHeight(3000);
        urlSteps.testing().path(MOTO).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Все марки")
    public void shouldSeeAllMarks() throws IOException {
        formsSteps.fillForm(formsSteps.getCategory().getBlock());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMark().getBlock()).button("Все марки").click();
        formsSteps.onFormsPage().footer().copyright().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage()
                        .unfoldedBlock(formsSteps.getMark().getBlock()));

        cookieSteps.deleteCookie("autoru_sid");
        cookieSteps.deleteCookie("autoruuid");
        urlSteps.onCurrentUrl().setProduction().open();
        formsSteps.fillForm(formsSteps.getCategory().getBlock());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMark().getBlock()).button("Все марки").click();
        formsSteps.onFormsPage().footer().copyright().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage()
                        .unfoldedBlock(formsSteps.getMark().getBlock()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Все модели")
    public void shouldSeeAllModels() throws IOException {
        formsSteps.fillForm(formsSteps.getMark().getBlock());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getModel().getBlock()).button("Все модели").click();
        formsSteps.onFormsPage().footer().copyright().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().unfoldedBlock(formsSteps.getModel().getBlock()));

        cookieSteps.deleteCookie("autoru_sid");
        cookieSteps.deleteCookie("autoruuid");
        urlSteps.onCurrentUrl().setProduction().open();
        formsSteps.fillForm(formsSteps.getMark().getBlock());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getModel().getBlock()).button("Все модели").click();
        formsSteps.onFormsPage().footer().copyright().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().unfoldedBlock(formsSteps.getModel().getBlock()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
