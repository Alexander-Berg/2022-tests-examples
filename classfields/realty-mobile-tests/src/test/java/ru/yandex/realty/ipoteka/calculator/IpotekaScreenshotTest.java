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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.MOBILE;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;

@DisplayName("Страница ипотечного калькулятора")
@Feature(MOBILE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class IpotekaScreenshotTest {

    private static final String DOWN_PAYMENT_SUM = "downPaymentSum";
    private static final String PERIOD_YEARS = "periodYears";
    private static final String PROPERTY_COST = "propertyCost";
    private static final String PROPERTY_COST_VALUE = "4500000";
    private static final String PERIOD_YEARS_VALUE = "20";
    private static final String DOWN_PAYMENT_SUM_VALUE = "1700000";
    private static final String RATE = "rate";
    private static final String RATE_VALUE = "7.7";
    public static final String YES = "YES";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(375, 5000);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот всей страницы")
    public void shouldSeePageScreenshot() {
        MockOffer offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .createWithDefaults();
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onIpotekaCalculatorPage().pageRoot());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onIpotekaCalculatorPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот фильтров")
    public void shouldSeeFilterScreenshot() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).queryParam(PROPERTY_COST, PROPERTY_COST_VALUE)
                .queryParam(PERIOD_YEARS, PERIOD_YEARS_VALUE).queryParam(DOWN_PAYMENT_SUM, DOWN_PAYMENT_SUM_VALUE)
                .queryParam(RATE, RATE_VALUE).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onIpotekaCalculatorPage().mortgageSearchFilters());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onIpotekaCalculatorPage().mortgageSearchFilters());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот результатов калькулятора")
    public void shouldSeeResultsScreenshot() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).queryParam(PROPERTY_COST, PROPERTY_COST_VALUE)
                .queryParam(PERIOD_YEARS, PERIOD_YEARS_VALUE).queryParam(DOWN_PAYMENT_SUM, DOWN_PAYMENT_SUM_VALUE)
                .queryParam(RATE, RATE_VALUE).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onIpotekaCalculatorPage().mortgageResults());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onIpotekaCalculatorPage().mortgageResults());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
