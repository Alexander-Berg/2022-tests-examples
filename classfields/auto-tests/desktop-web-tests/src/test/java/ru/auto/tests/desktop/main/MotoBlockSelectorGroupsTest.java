package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - блок «Мототехника» - группы селекторов")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MotoBlockSelectorGroupsTest {

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

    //@Parameter("Селект")
    @Parameterized.Parameter
    public String selectName;

    //@Parameter("Опция в селекте")
    @Parameterized.Parameter(1)
    public String selectItem;

    //@Parameter("Параметр")
    @Parameterized.Parameter(2)
    public String param;

    //@Parameter("Значение параметра")
    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Объем", "50 см³", "displacement", "50"},

                {"Год", "2018", "year", "2018"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Селектор от")
    public void shouldSelectFrom() {
        basePageSteps.onMainPage().motoBlock().selectGroupItem(selectName, "от", selectItem);
        basePageSteps.onMainPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onMainPage().motoBlock().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.path(MOTORCYCLE).path(ALL).addParam(format("%s_from", param), paramValue).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Селектор до")
    public void shouldSelectTo() {
        basePageSteps.onMainPage().motoBlock().selectGroupItem(selectName, "до", selectItem);
        basePageSteps.onMainPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onMainPage().motoBlock().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.path(MOTORCYCLE).path(ALL).addParam(format("%s_to", param), paramValue).shouldNotSeeDiff();
    }
}
