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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
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
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Главная - помощник - переключатель «Все/Новые/С пробегом»")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HelperBlockSectionSwitcherTest {

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

    @Parameterized.Parameter
    public String startSection;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String sectionUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Новые", "Все", ALL},
                {"Все", "Новые", NEW},
                {"Все", "С пробегом", USED}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().marksBlock().switcher("Помощник").click();
        basePageSteps.onMainPage().marksBlock().switcher(startSection).click();
        salesCount = basePageSteps.onMainPage().marksBlock().resultsButton().getText();
        basePageSteps.onMainPage().marksBlock().switcher(section).click();
        basePageSteps.onMainPage().marksBlock().resultsButton().waitUntil(not(hasText(salesCount)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кузову")
    public void shouldClickBody() {
        basePageSteps.onMainPage().marksBlock().body("Седан").click();
        basePageSteps.onMainPage().marksBlock().resultsButton().waitUntil(not(hasText(salesCount)));
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        urlSteps.path(CARS).path(sectionUrl).path("/body-sedan/").addParam("from", "old_guru").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по пресету")
    public void shouldClickPreset() {
        basePageSteps.onMainPage().marksBlock().preset("Быстрый").click();
        basePageSteps.onMainPage().marksBlock().resultsButton().waitUntil(not(hasText(salesCount)));
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        urlSteps.path(CARS).path(sectionUrl).addParam("acceleration_to", "7")
                .addParam("from", "old_guru").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Показать»")
    public void shouldClickResultsButton() {
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        urlSteps.path(CARS).path(sectionUrl).addParam("from", "old_guru").shouldNotSeeDiff();
    }
}