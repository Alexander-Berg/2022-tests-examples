package ru.yandex.realty.ipoteka.calculator;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
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
@DisplayName("Страница Ипотеки. Фильтр ставки")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class RateTest {

    private static final int CREDIT_RATE_RANGE = 15;
    private static final int MIN_CREDIT_YEAR = 1;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вводим срок кредита, должны увидеть в урле")
    public void shouldSeeNewCreditTermOnUrlByTextInput() {
        String rate = String.valueOf((int) (Math.random() * CREDIT_RATE_RANGE + MIN_CREDIT_YEAR));
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().rateInput().click();
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().rateInput().sendKeys(Keys.END);
        basePageSteps.clearInputByBackSpace(() ->
                basePageSteps.onIpotekaCalculatorPage().mortgageCalc().rateInput(), equalTo("0,00"));
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().rateInput().sendKeys(rate + "00");
        basePageSteps.onIpotekaCalculatorPage().headerOffers().click();
        urlSteps.queryParam("rate", rate).ignoreParam("downPaymentSum")
                .ignoreParam("propertyCost").ignoreParam("periodYears").shouldNotDiffWithWebDriverUrl();
    }
}