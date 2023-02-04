package ru.auto.tests.cabinet.priceReport;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.utils.Utils;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.PRICE_SUCCESSFULLY_UPDATED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.PRICE_REPORT;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.auto.tests.desktop.element.cabinet.priceReport.PriceReportOffer.WITHOUT_DISCOUNT;
import static ru.auto.tests.desktop.element.cabinet.priceReport.PriceReportOffer.WITH_DISCOUNT;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroup.dealerPriceGroup;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroups.dealerPriceGroups;
import static ru.auto.tests.desktop.mock.MockDealerPriceListing.dealerPriceListing;
import static ru.auto.tests.desktop.mock.MockDealerPriceOffer.dealerPriceOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_REPORT_PRICE_GROUPS;
import static ru.auto.tests.desktop.mock.Paths.DEALER_REPORT_PRICE_LISTING;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_PRICE_REPORT;
import static ru.auto.tests.desktop.utils.Utils.formatNumber;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.PRICE_REPORT)
@Story("Поля оффера отчёта")
@DisplayName("Поля оффера отчёта")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PriceReportOfferFieldsTest {

    private static final String MARK_ID = "BMW";
    private static final String MODEL_ID = "5ER";
    private static final String COMPLECTATION_ID = "22218457";
    private static final String CONFIGURATION_ID = "22213464";
    private static final String TECH_PARAM_ID = "22218461";
    private static final int YEAR = getRandomBetween(2000, 2020);

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain(IS_SHOWING_ONBOARDING_PRICE_REPORT, DATE_IN_PAST);

        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/ApiAccessClientManagerRole"),
                stub("cabinet/CommonCustomerGetAgency"),

                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setMarkId(MARK_ID)
                                                .setModelId(MODEL_ID)
                                                .setComplectationId(COMPLECTATION_ID)
                                                .setConfigurationId(CONFIGURATION_ID)
                                                .setTechParamId(TECH_PARAM_ID)
                                                .setYear(YEAR)
                                ).build())
        );

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(PRICE_REPORT).addParam(CLIENT_ID, "21413");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле оффера «Без скидки», скидок нет")
    public void shouldSeeWithoutDiscountFieldNoDiscounts() {
        int price = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_LISTING)
                        .withRequestQuery(
                                getPriceListingQuery())
                        .withResponseBody(
                                dealerPriceListing().setOffers(
                                        dealerPriceOffer()
                                                .setPrice(price)
                                                .setNoDiscounts()
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).click();

        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITHOUT_DISCOUNT).input().should(
                hasValue(formatNumber(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле оффера «Без скидки», скидки есть")
    public void shouldSeeWithoutDiscountFieldWithDiscounts() {
        int price = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_LISTING)
                        .withRequestQuery(
                                getPriceListingQuery())
                        .withResponseBody(
                                dealerPriceListing().setOffers(
                                        dealerPriceOffer()
                                                .setPrice(price)
                                                .setMaxDiscount(getRandomBetween(1000, 2000))
                                                .setCredit(getRandomBetween(1000, 2000))
                                                .setInsurance(getRandomBetween(1000, 2000))
                                                .setTradein(getRandomBetween(1000, 2000))
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).click();

        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITHOUT_DISCOUNT).input().should(
                hasValue(formatNumber(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле оффера «Со скидкой», скидок нет")
    public void shouldSeeWithDiscountFieldNoDiscounts() {
        int price = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_LISTING)
                        .withRequestQuery(
                                getPriceListingQuery())
                        .withResponseBody(
                                dealerPriceListing().setOffers(
                                        dealerPriceOffer()
                                                .setPrice(price)
                                                .setNoDiscounts()
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).click();

        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITH_DISCOUNT).staticValue().should(
                hasText(formatNumber(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле оффера «Со скидкой», скидки есть")
    public void shouldSeeWithDiscountFieldWithDiscounts() {
        int price = getRandomBetween(1000000, 2000000);
        int maxDiscount = getRandomBetween(3000, 5000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_LISTING)
                        .withRequestQuery(
                                getPriceListingQuery())
                        .withResponseBody(
                                dealerPriceListing().setOffers(
                                        dealerPriceOffer()
                                                .setPrice(price)
                                                .setMaxDiscount(maxDiscount)
                                                .setCredit(getRandomBetween(1000, 2000))
                                                .setInsurance(getRandomBetween(1000, 2000))
                                                .setTradein(getRandomBetween(1000, 2000))
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).click();

        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITH_DISCOUNT).staticValue().should(
                hasText(formatNumber(price - maxDiscount)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем поле оффера «Без скидки»")
    public void shouldChangeWithoutDiscountField() {
        int price = getRandomBetween(1000000, 2000000);
        String id = getRandomOfferId();

        JsonObject changePriceRequest = new JsonObject();
        changePriceRequest.addProperty("price", price);
        changePriceRequest.addProperty("currency", "RUR");

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_LISTING)
                        .withRequestQuery(
                                getPriceListingQuery())
                        .withResponseBody(
                                dealerPriceListing().setOffers(
                                        dealerPriceOffer()
                                                .setId(id)
                                                .setPrice(getRandomBetween(1000000, 2000000))
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).click();

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/price", USER_OFFERS_CARS, id))
                        .withRequestBody(
                                changePriceRequest)
                        .withStatusSuccessResponse()
        ).update();

        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITHOUT_DISCOUNT).clearInput();
        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITHOUT_DISCOUNT).input()
                .sendKeys(String.valueOf(price));
        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITH_DISCOUNT).click();

        steps.onPriceReportPage().notifier(PRICE_SUCCESSFULLY_UPDATED).should(isDisplayed());
        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITHOUT_DISCOUNT).input().should(
                hasValue(formatNumber(price)));
    }


    private Query getPriceListingQuery() {
        return query()
                .setMarkId(MARK_ID)
                .setModelId(MODEL_ID)
                .setComplectationId(COMPLECTATION_ID)
                .setConfigurationId(CONFIGURATION_ID)
                .setTechParamId(TECH_PARAM_ID)
                .setYear(String.valueOf(YEAR));
    }

}
