package ru.auto.tests.forms.footer;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FOOTER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Футер - ссылки")
@Feature(FOOTER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FooterUrlsTest {

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

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Пользовательское соглашение", YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE},
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MY).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        basePageSteps.onFormsPage().footer().button(urlTitle).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(url).shouldNotSeeDiff();
    }
}
