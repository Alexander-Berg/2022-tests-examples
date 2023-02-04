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
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;
import ru.auto.tests.passport.manager.AccountManager;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы - добавление отзыва после ленивой авторизации")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CarsAddLazyAuthTest {

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
    private PassportApiAdaptor passportApiAdaptor;

    @Before
    public void before() throws IOException {
        Account account = am.create();
        formsSteps.createReviewsCarsForm();
        formsSteps.setReg(false);
        urlSteps.testing().path(CARS).path(REVIEWS).path(ADD).open();
        formsSteps.fillForm(formsSteps.getReviewMinuses().getBlock());
        formsSteps.onFormsPage().unfoldedBlock("Войдите или зарегистрируйтесь, чтобы опубликовать отзыв")
                .input("Номер телефона", account.getLogin());
        formsSteps.onFormsPage().unfoldedBlock("Войдите или зарегистрируйтесь, чтобы опубликовать отзыв")
                .button("Подтвердить").click();
        waitSomething(3, TimeUnit.SECONDS);
        formsSteps.onFormsPage().unfoldedBlock("Войдите или зарегистрируйтесь, чтобы опубликовать отзыв")
                .input("Код из смс", passportApiAdaptor.getLastSmsCode(account.getLogin()));
        waitSomething(1, TimeUnit.SECONDS);
        formsSteps.onLkReviewsPage().header().avatar().waitUntil(isDisplayed());
        formsSteps.submitForm();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Добавление отзыва")
    public void shouldAddReview() {
        urlSteps.testing().path(MY).path(REVIEWS).shouldNotSeeDiff();
        formsSteps.onLkReviewsPage().getReview(0)
                .should(hasText("Заголовок отзыва\nAudi A4 I (B5) Рестайлинг 1.6 AT (101 л.с.)\n5,0\n" +
                        "Ожидает модерации\nРедактировать\nУдалить\n0\n0\n0\n0"));
    }
}