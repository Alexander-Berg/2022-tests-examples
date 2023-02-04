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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

//import io.qameta.allure.Parameter;

@DisplayName("Главная - блок марок - слайдер цены до")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MarksBlockSliderToTest {

    private String salesCount;

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Сдвиг слайдера")
    @Parameterized.Parameter
    public int offset;

    //@Parameter("Цена до")
    @Parameterized.Parameter(1)
    public String priceTo;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {50, "210000000"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        salesCount = basePageSteps.onMainPage().marksBlock().resultsButton().getText();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Слайдер до")
    public void shouldSlideTo() {
        basePageSteps.dragAndDrop(basePageSteps.onMainPage().marksBlock().sliderTo(), -offset, 0);
        basePageSteps.onMainPage().marksBlock().resultsButton().waitUntil(not(hasText(salesCount)));
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        urlSteps.path(CARS).path(ALL).addParam("price_to", priceTo).shouldNotSeeDiff();
    }
}