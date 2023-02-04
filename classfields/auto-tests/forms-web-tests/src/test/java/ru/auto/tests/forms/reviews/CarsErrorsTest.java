package ru.auto.tests.forms.reviews;

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
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы - отображение сообщений об ошибках")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CarsErrorsTest {

    private static final String VIDEO_LINK = "https://www.youtube.com/watch?v=hui";

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
    private AccountManager am;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        Account account = am.create();
        loginSteps.loginAs(account);
        formsSteps.createReviewsCarsForm();

        formsSteps.setWindowHeight(8000);
        urlSteps.testing().path(CARS).path(REVIEWS).path(ADD).open();
        formsSteps.fillForm(formsSteps.getModification().getBlock());
    }


    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сообщение об ошибке при попытке сохранить незаполненную форму")
    public void shouldSeeErrorMessage() throws IOException {
        formsSteps.submitForm();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        formsSteps.onFormsPage().radioButton("Мото").click();
        formsSteps.acceptAlert();
        urlSteps.setProduction().testing().path(CARS).path(REVIEWS).path(ADD).open();
        formsSteps.fillForm(formsSteps.getModification().getBlock());
        formsSteps.submitForm();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Сообщение об ошибке при добавлении видео с некорректной ссылкой")
    public void shouldSeeAddVideoErrorMessage() {
        formsSteps.onFormsPage().addVideoButton().click();
        formsSteps.onFormsPage().input("Ссылка на YouTube", VIDEO_LINK);
        formsSteps.onFormsPage().button("Добавить").click();
        formsSteps.onFormsPage().addVideoError().waitUntil(hasText("Ошибка добавления видео. Попробуйте еще раз."));
        formsSteps.onFormsPage().addedVideo().waitUntil(not(isDisplayed()));
    }
}
