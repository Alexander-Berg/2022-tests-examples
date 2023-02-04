package ru.auto.tests.forms.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.io.IOException;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@DisplayName("Отзывы - добавление отзыва")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CarsAddTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AccountManager am;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() throws IOException {
        Account account = am.create();
        loginSteps.loginAs(account);
        formsSteps.createReviewsCarsForm();
        urlSteps.testing().path(CARS).path(REVIEWS).path(ADD).open();
        formsSteps.fillForm(formsSteps.getReviewMinuses().getBlock());
        formsSteps.onFormsPage().unfoldedBlock("Ваши данные").input("Как к вам обращаться")
                .should(hasAttribute("value", format("id%s", account.getId())));
        formsSteps.submitForm();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @Issue("AUTORUFRONT-22091")
    @DisplayName("Добавление отзыва")
    public void shouldAddReview() {
        urlSteps.testing().path(MY).path(REVIEWS).shouldNotSeeDiff();
        formsSteps.onLkReviewsPage().reviewsList().should(hasSize(1));
        formsSteps.setWindowHeight(8000);
        formsSteps.onLkReviewsPage().getReview(0).button("Редактировать").click();
        formsSteps.onFormsPage().footer().copyright().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        urlSteps.onCurrentUrl().setProduction().open();
        formsSteps.onFormsPage().footer().copyright().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(formsSteps.onFormsPage().form());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
