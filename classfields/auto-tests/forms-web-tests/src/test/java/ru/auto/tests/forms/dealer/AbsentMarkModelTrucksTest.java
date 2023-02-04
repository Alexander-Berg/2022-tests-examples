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
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU_ADDING;

@DisplayName("Дилер, лёгкие коммерческие - нет моей марки/модели")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AbsentMarkModelTrucksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        Account account = formsSteps.linkUserToDealer();
        loginSteps.loginAs(account);
        formsSteps.createLcvForm();
        mockRule.newMock().with("desktop/UserDealer",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(TRUCKS).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Нет моей марки")
    public void shouldSendMarkRequest() throws IOException {
        formsSteps.fillForm(formsSteps.getCategory().getBlock());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMark().getBlock()).button("Все марки").click();
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getMark().getBlock()).button("Нет моей марки").click();

        urlSteps.switchToNextTab();
        urlSteps.fromUri(YANDEX_SUPPORT_AUTORU_ADDING).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Нет моей модели")
    public void shouldSendModelRequest() throws IOException {
        formsSteps.fillForm(formsSteps.getMark().getBlock());
        formsSteps.onFormsPage().unfoldedBlock(formsSteps.getModel().getBlock()).button("Нет моей модели").click();

        urlSteps.switchToNextTab();
        urlSteps.fromUri(YANDEX_SUPPORT_AUTORU_ADDING).shouldNotSeeDiff();
    }
}
