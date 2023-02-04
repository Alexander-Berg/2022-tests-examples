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
import org.openqa.selenium.Keys;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;

/**
 * @author kantemirov
 */
@DisplayName("Страница Ипотеки. Фильтр начального платежа")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class InitialPaymentTest {

    private static final int COST_TO = 8000000;
    private static final int MINIMAL_INITIAL_PAYMENT = COST_TO / 100 * 15;
    private static final double INITIAL_PAYMENT_RANGE = 0.85;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().costInput().clear();
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().costInput().sendKeys(String.valueOf(COST_TO));
        basePageSteps.onIpotekaCalculatorPage().headerOffers().click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вводим стоимость квартиры до..., должны увидеть начальный платеж 15%")
    public void shouldSeeInitialPaymentInInput() {
        int initialPaymentFromInput = Integer.parseInt(basePageSteps.onIpotekaCalculatorPage().mortgageCalc()
                .downpaymentInput().getAttribute("value").replaceAll("\u00A0", ""));
        assertThat(initialPaymentFromInput).isEqualTo(MINIMAL_INITIAL_PAYMENT);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вводим начальный платеж, должны увидеть в урле")
    public void shouldSeeInitialPaymentInUrl() {
        int initialPayment = (int) (Math.random() * COST_TO * INITIAL_PAYMENT_RANGE + (COST_TO / 15));
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().downpaymentInput().clear();
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().downpaymentInput()
                .sendKeys(String.valueOf(initialPayment) + Keys.ENTER);
        basePageSteps.onIpotekaCalculatorPage().headerOffers().click();

        urlSteps.queryParam("downPaymentSum", String.valueOf(initialPayment))
                .ignoreParam("periodYears").ignoreParam("propertyCost").ignoreParam("rate")
                .shouldNotDiffWithWebDriverUrl();
    }
}