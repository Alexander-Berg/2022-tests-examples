package ru.auto.tests.bem.header;

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
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.TestData.OWNER_USER_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Шапка - чат")
@Feature(HEADER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ChatTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private LoginSteps loginSteps;

    //@Parameter("Стартовая страница")
    @Parameterized.Parameter
    public String page;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CATALOG},
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(page).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по иконке чата под незарегом")
    public void shouldClickChatButtonUnauth() {
        basePageSteps.setWideWindowSize();
        basePageSteps.onCatalogPage().header().chatButton().click();
        basePageSteps.onCatalogPage().chatAuthPopup().waitUntil("Чат не открылся", isDisplayed(), 5);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по иконке чата под зарегом")
    public void shouldClickChatButtonAuth() throws IOException {
        loginSteps.loginAs(OWNER_USER_PROVIDER.get());
        basePageSteps.onCatalogPage().header().chatButton().click();
        basePageSteps.onCatalogPage().chat().waitUntil("Чат не открылся", isDisplayed(), 5);
    }
}
