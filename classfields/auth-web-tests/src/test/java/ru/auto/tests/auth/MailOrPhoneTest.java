package ru.auto.tests.auth;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTH;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.page.auth.AuthPage.CONTINUE_BUTTON;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Тест на корректные адреса почты и номера телефонов")
@Feature(AUTH)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)

@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MailOrPhoneTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String Mail;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"79271056173@mail.ru"},
                {"koluzganovakn@edu.hse.ru"},
                {"koluzganovak@gmail.com"},
                {"79890010397"},
                {"89270010397"},
                {"9000010397"},
                {"+7 9260010397"},
                {"+7(800)0010397"},
                {"8-909-001-03-97"},
        });
    }

    @Before
    public void before() {
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(urlSteps.getConfig().getTestingURI().toString())).open();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Проверка на корректно введенную почту или номер телефона")
    public void shouldVoidCorrectMailOrNumber() {
        basePageSteps.onAuthPage().phoneInput().sendKeys(Mail);
        basePageSteps.onAuthPage().button(CONTINUE_BUTTON).click();

        basePageSteps.onAuthPage().notifyCode().waitUntil(isDisplayed());
    }
}
