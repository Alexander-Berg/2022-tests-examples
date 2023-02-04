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
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
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
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Главная - помощник - пресеты")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HelperBlockPresetsTest {

    private String salesCount;

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

    //@Parameter("Название пресета")
    @Parameterized.Parameter
    public String presetTitle;

    //@Parameter("Запрос")
    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Вместительный", "%s/moskva/cars/all/?catalog_equipment=seats-5%%2Cseats-6%%2Cseats-7%%2Cseats-8%%2Cseats-9%%2Cthird-row-seats" +
                        "&trunk_volume_from=500"},
                {"Проходимый", "%s/moskva/cars/all/drive-4x4_wheel/?clearance_from=200"},
                {"Экономичный", "%s/moskva/cars/all/?fuel_rate_to=7"},
                {"Быстрый", "%s/moskva/cars/all/?acceleration_to=7"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().marksBlock().switcher("Помощник").click();
        salesCount = basePageSteps.onMainPage().marksBlock().resultsButton().getText();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по пресету")
    public void shouldClickPreset() {
        basePageSteps.onMainPage().marksBlock().preset(presetTitle).click();
        basePageSteps.onMainPage().marksBlock().resultsButton().waitUntil(not(hasText(salesCount)));
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        urlSteps.fromUri(format(url, urlSteps.getConfig().getTestingURI())).addParam("from", "old_guru")
                .shouldNotSeeDiff();
    }
}