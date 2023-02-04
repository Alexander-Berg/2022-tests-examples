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
import static ru.auto.tests.desktop.consts.Urls.YANDEX_LEGAL_CONFIDENTIAL;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Пользоватеское соглашение")
@Feature(AUTH)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)

@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UserAgreementTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String urlTitle;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"пользовательского соглашения", YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE},
                {"пользовательским соглашением", YANDEX_LEGAL_CONFIDENTIAL}
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
    @DisplayName("Переход на пользовательское соглашение")
    public void shouldClickOnTheAgreementUrl() {
        basePageSteps.onAuthPage().button(urlTitle).waitUntil(isDisplayed()).click();

        urlSteps.fromUri(url).shouldNotSeeDiff();
    }
}
