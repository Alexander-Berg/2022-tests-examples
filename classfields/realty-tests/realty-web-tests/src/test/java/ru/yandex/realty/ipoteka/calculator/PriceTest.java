package ru.yandex.realty.ipoteka.calculator;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
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

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;

/**
 * @author kantemirov
 */
@DisplayName("Страница Ипотеки. Фильтры цены")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PriceTest {

    private static final String COST_TO = "4000000";

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
        basePageSteps.onIpotekaCalculatorPage().mortgageCalc().costInput().sendKeys(COST_TO);
        basePageSteps.onIpotekaCalculatorPage().headerOffers().click();
    }

    @Test
    @Owner(KANTEMIROV)
    @Description("Выставляем цену, должны увидеть в урле")
    public void shouldSeeNewRateForOldFlat() {
        urlSteps.queryParam("propertyCost", COST_TO)
                .ignoreParam("rate").ignoreParam("downPaymentSum").ignoreParam("periodYears")
                .shouldNotDiffWithWebDriverUrl();
    }
}