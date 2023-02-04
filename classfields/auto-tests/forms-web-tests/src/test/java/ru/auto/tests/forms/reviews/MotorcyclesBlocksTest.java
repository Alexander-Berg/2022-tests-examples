package ru.auto.tests.forms.reviews;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;

@DisplayName("Отзывы, мотоциклы - отображение формы")
@Feature(FORMS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MotorcyclesBlocksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String index;

    @Parameterized.Parameter(1)
    public String block;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"category", "Категория"},
                {"mark", "Укажите марку"},
                {"model", "Укажите модель"},
                {"year", "Год выпуска"},
                {"own", "Срок владения"},
                {"review", "Ваш отзыв"},
                {"minuses", "Минусы"}
        });
    }

    @Before
    public void before() {
        formsSteps.createReviewsMotorcyclesForm();
        formsSteps.setReg(false);
        formsSteps.phone.setValue(getRandomPhone());

        formsSteps.setWindowHeight(8000);
        urlSteps.testing().path(MOTO).path(REVIEWS).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение формы")
    @Category({Regression.class, Screenshooter.class})
    public void shouldSeeForm() throws IOException {
        formsSteps.fillForm(block);
        formsSteps.onFormsPage().footer().copyright().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        cookieSteps.deleteCookie("autoru_sid");
        cookieSteps.deleteCookie("autoruuid");
        urlSteps.setProduction().testing().path(MOTO).path(REVIEWS).path(ADD).open();
        formsSteps.fillForm(block);
        formsSteps.onFormsPage().footer().copyright().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
