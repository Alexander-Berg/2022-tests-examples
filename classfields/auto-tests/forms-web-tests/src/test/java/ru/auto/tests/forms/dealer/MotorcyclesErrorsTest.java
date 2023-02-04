package ru.auto.tests.forms.dealer;

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
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;
import ru.auto.tests.passport.account.Account;

import javax.inject.Inject;
import java.io.IOException;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Дилер, мотоциклы - отображение сообщений об ошибках")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MotorcyclesErrorsTest {

    private Account account;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        account = formsSteps.linkUserToDealer();
        loginSteps.loginAs(account);

        urlSteps.testing().path(MOTO).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сообщение об ошибке при попытке сохранить незаполненную форму нового ТС")
    public void shouldSeeNewFormErrorMessage() throws IOException {
        formsSteps.createMotorcyclesDealerNewForm();
        formsSteps.fillForm(formsSteps.getCategory().getBlock());
        formsSteps.submitForm();
        formsSteps.onFormsPage().submitErrorMessage().waitUntil(hasText("Пропущены обязательные поля или заполнены " +
                "неправильно: марка, модель, цена, цвет, год выпуска, объём двигателя."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сообщение об ошибке при попытке сохранить незаполненную форму б/у ТС")
    public void shouldSeeUsedFormErrorMessage() throws IOException {
        formsSteps.createMotorcyclesDealerUsedForm();
        formsSteps.fillForm(formsSteps.getCategory().getBlock());
        formsSteps.submitForm();
        formsSteps.onFormsPage().submitErrorMessage().waitUntil(hasText("Пропущены обязательные поля или заполнены " +
                "неправильно: марка, модель, цена, цвет, год выпуска, пробег, объём двигателя."));
    }
}
