package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Вкладки")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TabsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Parameterized.Parameter
    public String tab;

    @Parameterized.Parameter(1)
    public String tabUrl;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Легковые", "https://%s/my/cars/"},
                {"Мото", "https://%s/my/moto/"},
                {"Коммерческие", "https://%s/my/trucks/"}
        });
    }

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);
    }

    @Test
    @Category({Regression.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик по вкладке")
    public void shouldClickTab() {
        urlSteps.testing().path(MY).open();
        basePageSteps.onLkSalesNewPage().radioButtonGroup().button(tab).should(isDisplayed()).click();

        urlSteps.fromUri(format(tabUrl, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }

}
