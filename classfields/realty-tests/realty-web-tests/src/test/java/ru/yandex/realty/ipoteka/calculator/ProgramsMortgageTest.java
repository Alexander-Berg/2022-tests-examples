package ru.yandex.realty.ipoteka.calculator;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.rules.MockRuleConfigurable;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;
import static ru.yandex.realty.element.ipoteka.MortgageProgram.MORE_BUTTON;
import static ru.yandex.realty.element.ipoteka.MortgageProgram.REGISTER_BUTTON;

/**
 * @author kantemirov
 */
@DisplayName("Страница Ипотеки. Ипотечные программы")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ProgramsMortgageTest {

    private static final String CONTAINER_EXPANDED = "containerExpanded";
    private static final int INDEX = 2;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRuleConfigurable.mortgageProgramSearch().mortgageProgramCalculator().createWithDefaults();
        urlSteps.testing().testing().path(IPOTEKA_CALCULATOR).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Подходящие программы кликаем и переходим к фрагменту")
    public void shouldSeePrograms() {
        basePageSteps.onIpotekaCalculatorPage().button("программ").click();
        urlSteps.fragment("mortgage-programs").ignoreParam("redirect_from_rgid")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «оформить»")
    public void shouldSeeAlfaPopup() {
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().hover();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().button(REGISTER_BUTTON).click();
        basePageSteps.onIpotekaCalculatorPage().alfaPopup().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «рассчитать»")
    public void shouldSeeMortgageBank() {
        basePageSteps.onIpotekaCalculatorPage().mortgagesPrograms().waitUntil(hasSize(greaterThan(INDEX))).get(INDEX).hover();
        basePageSteps.onIpotekaCalculatorPage().mortgagesPrograms().get(INDEX).link(MORE_BUTTON).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.fromUri("https://absolutbank.ru/personal/loans/mortgage/gosprogramma/")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Разворачиваем программу")
    public void shouldSeeProgramsExpand() {
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().hover();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().expand().click();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().should(hasClass(containsString(CONTAINER_EXPANDED)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сворачиваем программу")
    public void shouldSeeProgramsNotExpand() {
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().hover();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().expand().click();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().waitUntil(hasClass(containsString(CONTAINER_EXPANDED)));
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().expand().click();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().should(hasClass(not(containsString(CONTAINER_EXPANDED))));
    }
}
