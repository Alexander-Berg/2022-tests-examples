package ru.auto.tests.mobile.header;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Шапка")
@Feature(HEADER)
public class HeaderCatalogTest {

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
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/catalog/cars/"},
                {"/catalog/cars/bmw/"},
                {"/catalog/cars/bmw/2er/"},
                {"/catalog/cars/bmw/2er/20067958/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(url).open();
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Клик по логотипу")
    @Owner(DSVICHIHIN)
    public void shouldClickLogo() {
        basePageSteps.onCatalogPage().header().logo().should(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @DisplayName("Клик по кнопке сайдбара")
    @Owner(DSVICHIHIN)
    public void shouldClickSidebarButton() {
        basePageSteps.onCatalogPage().header().sidebarButton().click();
        basePageSteps.onCatalogPage().sidebar().waitUntil(isDisplayed());
    }
}
