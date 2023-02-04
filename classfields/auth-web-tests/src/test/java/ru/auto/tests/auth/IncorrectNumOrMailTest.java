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
import ru.auto.tests.desktop.rule.MockRule;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Проверка на некорректный ввод номера телефона и почты")
@Feature(AUTH)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)

@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class IncorrectNumOrMailTest {

    private static final String INCOMPLETE_PHONE_MAIL_ERROR = "Укажите телефон или адрес электронной почты";
    private static final String INCORRECT_PHONE_ERROR = "Неправильный номер телефона";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String numAndMail;

    @Parameterized.Parameter(1)
    public String answer;

    @Parameterized.Parameters(name = "name = {index}: {0}, {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"+799900139", INCOMPLETE_PHONE_MAIL_ERROR},
                {"999001039", INCOMPLETE_PHONE_MAIL_ERROR},
                {"899900781039", INCORRECT_PHONE_ERROR},
                {"7927105@mail", INCOMPLETE_PHONE_MAIL_ERROR},
                {"7927ni@.ru", INCOMPLETE_PHONE_MAIL_ERROR},
                {"eubvweiufe@hbvru", INCOMPLETE_PHONE_MAIL_ERROR}
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
    @DisplayName("Проверка номера")
    public void shouldVoidInCorrectNumberOrMail() {
        basePageSteps.onAuthPage().phoneInput().sendKeys(numAndMail);
        basePageSteps.onAuthPage().button(CONTINUE_BUTTON).click();

        basePageSteps.onAuthPage().notifyError().waitUntil(hasText(answer));
    }
}
