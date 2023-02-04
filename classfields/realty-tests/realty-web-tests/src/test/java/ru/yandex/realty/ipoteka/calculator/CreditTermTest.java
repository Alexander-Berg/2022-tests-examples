package ru.yandex.realty.ipoteka.calculator;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;

/**
 * @author kantemirov
 */
@DisplayName("Страница Ипотеки. Фильтры срока кредита")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CreditTermTest {

    private static final int CREDIT_YEAR_RANGE = 30;
    private static final int MIN_CREDIT_YEAR = 10;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Вводим срок кредита, должны увидеть в урле")
    public void shouldSeeNewCreditTermOnUrlByTextInput() {
        String creditTerm = String.valueOf((int) (Math.random() * CREDIT_YEAR_RANGE + MIN_CREDIT_YEAR));
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().inputCreditTerm().click();
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().inputCreditTerm().sendKeys(Keys.END);
        basePageSteps.clearInputByBackSpace(() ->
                basePageSteps.onIpotekaCalculatorPage().mortgageCalc().inputCreditTerm(), equalTo("0"));
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().inputCreditTerm().sendKeys(creditTerm);
        basePageSteps.onIpotekaCalculatorPage().headerOffers().click();
        urlSteps.queryParam("periodYears", creditTerm).ignoreParam("downPaymentSum")
                .ignoreParam("propertyCost").ignoreParam("rate").shouldNotDiffWithWebDriverUrl();
    }
}