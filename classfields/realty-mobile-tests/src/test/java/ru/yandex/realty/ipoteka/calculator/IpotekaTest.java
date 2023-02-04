package ru.yandex.realty.ipoteka.calculator;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.MOBILE;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Страница ипотечного калькулятора")
@Feature(MOBILE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class IpotekaTest {

    private static final String DOWN_PAYMENT_SUM = "downPaymentSum";
    private static final String PERIOD_YEARS = "periodYears";
    private static final String PROPERTY_COST = "propertyCost";
    private static final String PROPERTY_COST_VALUE = "3400000";
    private static final String PERIOD_YEARS_VALUE = "20";
    private static final String DOWN_PAYMENT_SUM_VALUE = "1700000";
    private static final String RATE = "rate";
    private static final String RATE_VALUE = "7.7";
    private static final String PROPERTY_COST_NAME = "Цена недвижимости";
    private static final String PERIOD_YEARS_NAME = "Срок кредита";
    private static final String DOWN_PAYMENT_SUM_NAME = "Первоначальный взнос";
    private static final String RATE_NAME = "Ставка";
    private static final Matcher<String> TO_ZERO_MATCHER = equalTo("0");

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.resize(375, 800);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по офферу ипотеки")
    public void shouldSeeOfferClick() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        String url = basePageSteps.onIpotekaCalculatorPage().ipotekaOffer().waitUntil(hasSize(greaterThan(0))).get(0).link()
                .getAttribute("href");
        basePageSteps.onIpotekaCalculatorPage().ipotekaOffer().get(0).link().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.fromUri(url).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Смотреть все предложения»")
    public void shouldSeeRelatedPrograms() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().link("Смотреть все предложения").click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA)
                .queryParam(UrlSteps.SHOW_SIMILAR_URL_PARAM, UrlSteps.NO_VALUE)
                .ignoreParam(UrlSteps.PRICE_MIN_URL_PARAM).ignoreParam(UrlSteps.PRICE_MAX_URL_PARAM)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по подходящим программам")
    public void shouldSeeAllOffers() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().button("подходящ").click();
        urlSteps.fragment("mortgage-programs").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Оформить» видим модуль Альфабанка")
    public void shouldSeeFirstBankOffer() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().bankOffers().waitUntil(hasSize(greaterThan(0))).get(FIRST)
                .button("Оформить").click();
        basePageSteps.onIpotekaCalculatorPage().alfaModal().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Рассчитать»")
    public void shouldSeeBankOffer() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().bankOffers().waitUntil(hasSize(greaterThan(10))).get(10)
                .link("Рассчитать").click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path("/ipoteka/promsvyazbank-323157/semejnaya-ipoteka-2420669/")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Бегунок стоимости квартиры")
    public void shouldSeePrice() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.moveSlider(basePageSteps.onIpotekaCalculatorPage().runner(PROPERTY_COST_NAME), 200, 0);
        urlSteps.ignoreParam(DOWN_PAYMENT_SUM).queryParam(PROPERTY_COST, "100810000")
                .ignoreParam(PERIOD_YEARS).ignoreParam(RATE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Инпут стоимости квартиры")
    public void shouldSeePriceInput() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().input(PROPERTY_COST_NAME).click();
        basePageSteps.onIpotekaCalculatorPage().input(PROPERTY_COST_NAME).sendKeys(Keys.END);
        basePageSteps.clearInputByBackSpace(() ->
                basePageSteps.onIpotekaCalculatorPage().input(PROPERTY_COST_NAME), TO_ZERO_MATCHER);
        basePageSteps.onIpotekaCalculatorPage().input(PROPERTY_COST_NAME).sendKeys(PROPERTY_COST_VALUE);
        basePageSteps.onIpotekaCalculatorPage().header().click();
        urlSteps.ignoreParam(DOWN_PAYMENT_SUM).queryParam(PROPERTY_COST, PROPERTY_COST_VALUE)
                .ignoreParam(PERIOD_YEARS).ignoreParam(RATE).shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Бегунок срока кредита")
    public void shouldSeeCreditTerm() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.moveSlider(basePageSteps.onIpotekaCalculatorPage().runner(PERIOD_YEARS_NAME), 200, 0);
        urlSteps.ignoreParam(DOWN_PAYMENT_SUM).ignoreParam(PROPERTY_COST)
                .queryParam(PERIOD_YEARS, "46").ignoreParam(RATE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Инпут срока кредита")
    public void shouldSeeCreditTermInput() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().input(PERIOD_YEARS_NAME).click();
        basePageSteps.onIpotekaCalculatorPage().input(PERIOD_YEARS_NAME).sendKeys(Keys.END);
        basePageSteps.clearInputByBackSpace(() ->
                basePageSteps.onIpotekaCalculatorPage().input(PERIOD_YEARS_NAME), TO_ZERO_MATCHER);
        basePageSteps.onIpotekaCalculatorPage().input(PERIOD_YEARS_NAME).sendKeys(PERIOD_YEARS_VALUE);
        basePageSteps.onIpotekaCalculatorPage().header().click();
        urlSteps.ignoreParam(DOWN_PAYMENT_SUM).ignoreParam(PROPERTY_COST)
                .queryParam(PERIOD_YEARS, PERIOD_YEARS_VALUE).ignoreParam(RATE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Бегунок начального платежа")
    public void shouldSeeDownPaymentSum() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.moveSlider(basePageSteps.onIpotekaCalculatorPage().runner(DOWN_PAYMENT_SUM_NAME), 200, 0);
        urlSteps.queryParam(DOWN_PAYMENT_SUM, "7320000").ignoreParam(PROPERTY_COST)
                .ignoreParam(PERIOD_YEARS).ignoreParam(RATE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Инпут начального платежа")
    public void shouldSeeDownPaymentSumInput() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().input(DOWN_PAYMENT_SUM_NAME).click();
        basePageSteps.onIpotekaCalculatorPage().input(DOWN_PAYMENT_SUM_NAME).sendKeys(Keys.END);
        basePageSteps.clearInputByBackSpace(() ->
                basePageSteps.onIpotekaCalculatorPage().input(DOWN_PAYMENT_SUM_NAME), TO_ZERO_MATCHER);
        basePageSteps.onIpotekaCalculatorPage().input(DOWN_PAYMENT_SUM_NAME).sendKeys(DOWN_PAYMENT_SUM_VALUE);
        basePageSteps.onIpotekaCalculatorPage().header().click();
        urlSteps.queryParam(DOWN_PAYMENT_SUM, DOWN_PAYMENT_SUM_VALUE).ignoreParam(PROPERTY_COST)
                .ignoreParam(PERIOD_YEARS).ignoreParam(RATE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Бегунок ставки")
    public void shouldSeeRate() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.moveSlider(basePageSteps.onIpotekaCalculatorPage().runner(RATE_NAME), 200, 0);
        urlSteps.ignoreParam(DOWN_PAYMENT_SUM).ignoreParam(PROPERTY_COST)
                .ignoreParam(PERIOD_YEARS).queryParam(RATE, "17").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Инпут ставки")
    public void shouldSeeRateInput() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().input(RATE_NAME).click();
        basePageSteps.onIpotekaCalculatorPage().input(RATE_NAME).sendKeys(Keys.END);
        basePageSteps.clearInputByBackSpace(() ->
                basePageSteps.onIpotekaCalculatorPage().input(RATE_NAME), equalTo("0,00"));
        basePageSteps.onIpotekaCalculatorPage().input(RATE_NAME).sendKeys("770");
        basePageSteps.onIpotekaCalculatorPage().header().click();
        urlSteps.ignoreParam(DOWN_PAYMENT_SUM).ignoreParam(PROPERTY_COST)
                .ignoreParam(PERIOD_YEARS).queryParam(RATE, RATE_VALUE).shouldNotDiffWithWebDriverUrl();
    }
}
