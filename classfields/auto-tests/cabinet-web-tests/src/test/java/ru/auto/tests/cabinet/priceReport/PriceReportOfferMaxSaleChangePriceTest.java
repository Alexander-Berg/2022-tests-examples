package ru.auto.tests.cabinet.priceReport;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.dealerOfferDiscounts.DiscountOptions;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.utils.Utils;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.SALE_SUCCESSFULLY_UPDATED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.PRICE_REPORT;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.auto.tests.desktop.element.cabinet.priceReport.PriceReportOffer.CREDIT;
import static ru.auto.tests.desktop.element.cabinet.priceReport.PriceReportOffer.KASKO;
import static ru.auto.tests.desktop.element.cabinet.priceReport.PriceReportOffer.MAXIMUM;
import static ru.auto.tests.desktop.element.cabinet.priceReport.PriceReportOffer.TRADE_IN;
import static ru.auto.tests.desktop.element.cabinet.priceReport.PriceReportOffer.WITH_DISCOUNT;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroup.dealerPriceGroup;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroups.dealerPriceGroups;
import static ru.auto.tests.desktop.mock.MockDealerPriceListing.dealerPriceListing;
import static ru.auto.tests.desktop.mock.MockDealerPriceOffer.dealerPriceOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_REPORT_PRICE_GROUPS;
import static ru.auto.tests.desktop.mock.Paths.DEALER_REPORT_PRICE_LISTING;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.beans.dealerOfferDiscounts.DiscountOptions.discountOptions;
import static ru.auto.tests.desktop.mock.beans.dealerOfferDiscounts.DiscountOptionsRequest.discountOptionsRequest;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_PRICE_REPORT;
import static ru.auto.tests.desktop.utils.Utils.formatNumber;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.PRICE_REPORT)
@Story("Скидки оффера отчёта")
@DisplayName("Скидки оффера отчёта")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PriceReportOfferMaxSaleChangePriceTest {

    private static final String MARK_ID = "BMW";
    private static final String MODEL_ID = "5ER";
    private static final String COMPLECTATION_ID = "22218457";
    private static final String CONFIGURATION_ID = "22213464";
    private static final String TECH_PARAM_ID = "22218461";
    private static final int YEAR = getRandomBetween(2000, 2020);
    private static final String ID = getRandomOfferId();

    private static final int PRICE = getRandomBetween(1000000, 2000000);
    private static final int CHANGED_DISCOUNT = getRandomBetween(3000, 5000);

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

    @Parameterized.Parameter
    public String blockName;

    @Parameterized.Parameter(1)
    public int price;

    @Parameterized.Parameter(2)
    public DiscountOptions changedDiscountOption;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {MAXIMUM, PRICE - CHANGED_DISCOUNT,
                        discountOptions().setMaxDiscount(CHANGED_DISCOUNT)},
                {TRADE_IN, PRICE,
                        discountOptions().setTradein(CHANGED_DISCOUNT)},
                {CREDIT, PRICE,
                        discountOptions().setCredit(CHANGED_DISCOUNT)},
                {KASKO, PRICE,
                        discountOptions().setInsurance(CHANGED_DISCOUNT)}
        });
    }

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
                                ).build()),

                stub().withGetDeepEquals(DEALER_REPORT_PRICE_LISTING)
                        .withRequestQuery(
                                getPriceListingQuery())
                        .withResponseBody(
                                dealerPriceListing().setOffers(
                                        dealerPriceOffer().setId(ID)
                                                .setPrice(PRICE)
                                ).build())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(PRICE_REPORT).addParam(CLIENT_ID, "21413").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливаем скидку, поле «Со скидкой» меняется только при установке максимальной скидки")
    public void shouldSeeWithDiscountPrice() {
        steps.onPriceReportPage().tableRows().get(0).click();

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s/attribute", USER_OFFERS_CARS, ID))
                        .withRequestBody(getJsonObject(
                                discountOptionsRequest().setDiscountOptions(changedDiscountOption)))
                        .withStatusSuccessResponse()
        ).update();

        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(blockName).input().sendKeys(
                String.valueOf(CHANGED_DISCOUNT));
        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITH_DISCOUNT).click();
        steps.onPriceReportPage().notifier(SALE_SUCCESSFULLY_UPDATED).waitUntil(isDisplayed());

        steps.onPriceReportPage().tableRows().get(0).offersTable().offers().get(0).inputBlock(WITH_DISCOUNT).staticValue().should(
                hasText(formatNumber(price)));
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
