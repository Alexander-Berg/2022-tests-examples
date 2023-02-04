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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;

@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Подшапка")
@Feature(HEADER)
public class SubHeaderTest {

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
    public String tabTitle;

    @Parameterized.Parameter(1)
    public String tabUrl;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Транспорт", "https://%s/moskva/cars/all/"},
                {"ПроАвто", "https://%s/history/?from=shapka"},
                {"Журнал", "https://mag.%s/?from=top_menu_secondline"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке")
    public void shouldClickTab() {
        basePageSteps.onMainPage().subHeader().url(tabTitle).click();
        urlSteps.fromUri(format(tabUrl, urlSteps.getConfig().getBaseDomain())).ignoreParam("geo_id")
                .ignoreParam("rgid").shouldNotSeeDiff();
    }
}
