package ru.auto.tests.cabinet.priceReport;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.utils.Utils;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.PRICE_REPORT;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.BODY_TYPE_GROUP;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.auto.tests.desktop.consts.QueryParams.COMPLECTATION_NAME;
import static ru.auto.tests.desktop.consts.QueryParams.GEAR_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.TECH_PARAM_ID;
import static ru.auto.tests.desktop.consts.QueryParams.TRANSMISSION;
import static ru.auto.tests.desktop.consts.QueryParams.YEAR_FROM;
import static ru.auto.tests.desktop.consts.QueryParams.YEAR_TO;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroup.dealerPriceGroup;
import static ru.auto.tests.desktop.mock.MockDealerPriceGroups.dealerPriceGroups;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_REPORT_PRICE_GROUPS;
import static ru.auto.tests.desktop.page.cabinet.CabinetPriceReportPage.THIS_ONE;
import static ru.auto.tests.desktop.step.CookieSteps.DATE_IN_PAST;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_ONBOARDING_PRICE_REPORT;
import static ru.auto.tests.desktop.utils.Utils.formatNumber;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.PRICE_REPORT)
@Story("Поля отчёта")
@DisplayName("Поля отчёта")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PriceReportFieldsTest {

    private static final String PRICE_POPUP_TEMPLATE = "Минимальная цена на рынке\n" +
            "Без скидки\n%s ₽\n" +
            "Со скидкой\n%s ₽\n" +
            "Максимальные скидки на рынке\n" +
            "КАСКО\nTrade-in\n%s ₽\n" +
            "Кредит\nМаксимальная\n%s ₽\n" +
            "Максимальные скидки, которые есть на рынке у одного из конкурентов. Сегодня это вот этот";

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
                stub("cabinet/CommonCustomerGetAgency")
        );

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(PRICE_REPORT).addParam(CLIENT_ID, "21413");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Марка, модель» ")
    public void shouldSeeMarkModelColumnText() {
        String mark = getRandomString();
        String model = getRandomString();
        String superGen = getRandomString();
        String configuration = getRandomString();
        String techParam = getRandomString();

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setMark(mark)
                                                .setModel(model)
                                                .setSuperGen(superGen)
                                                .setConfiguration(configuration)
                                                .setTechParam(techParam)
                                                .setTransmission("ROBOT")
                                                .setDrive("FORWARD_CONTROL")
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).markModel().should(hasText(format(
                "%s %s %s\n%s\u2009/\u2009%s\u2009/\u2009Бензин\u2009/\u2009роботизированная\u2009/\u2009передний",
                mark, model, superGen, configuration, techParam
        )));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Комплектация»")
    public void shouldSeeComplectationColumnText() {
        String complectation = getRandomString();

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setComplectation(complectation)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).complectation().should(hasText(complectation));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Год»")
    public void shouldSeeYearColumnText() {
        int year = getRandomBetween(2000, 2020);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setYear(year)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).year().should(hasText(String.valueOf(year)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Ваш склад - в наличии/под заказ»")
    public void shouldSeeWarehouseAvailableColumnText() {
        int inStock = getRandomBetween(1, 50);
        int onOrder = getRandomBetween(1, 50);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setDealerInStock(inStock)
                                                .setDealerOnOrder(onOrder)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).warehouseColumn().availability().should(hasText(format(
                "%d\u2009/\u2009%d", inStock, onOrder)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Ваш склад - мин. цена со скидкой»")
    public void shouldSeeWarehouseMinPriceDiscountText() {
        int price = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setDealerMinPriceDiscount(price)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).warehouseColumn().minPriceDiscount().should(hasText(
                formatNumber(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Ваш склад - мин. цена без скидки»")
    public void shouldSeeWarehouseMinPriceText() {
        int price = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setDealerMinPrice(price)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).warehouseColumn().minPrice().should(hasText(
                formatNumber(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Ваш склад - макс. цена»")
    public void shouldSeeWarehouseMaxPriceText() {
        int price = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setDealerMaxPrice(price)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).warehouseColumn().maxPrice().should(hasText(
                formatNumber(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Конкуренты - в наличии/под заказ»")
    public void shouldSeeCompetitorAvailableColumnText() {
        int inStock = getRandomBetween(1, 50);
        int onOrder = getRandomBetween(1, 50);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setCompetitorInStock(inStock)
                                                .setCompetitorOnOrder(onOrder)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).competitorColumn().availability().should(hasText(format(
                "%d\u2009/\u2009%d", inStock, onOrder)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Конкуренты - мин. цена со скидкой»")
    public void shouldSeeCompetitorMinPriceDiscountText() {
        int price = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setCompetitorMinPriceDiscount(price)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).competitorColumn().minPriceDiscount().should(hasText(
                formatNumber(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Конкуренты - мин. цена без скидки»")
    public void shouldSeeCompetitorMinPriceText() {
        int price = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setCompetitorMinPrice(price)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).competitorColumn().minPrice().should(hasText(
                formatNumber(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст столбца «Конкуренты - макс. цена»")
    public void shouldSeeCompetitorMaxPriceText() {
        int price = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setCompetitorMaxPrice(price)
                                ).build())
        ).create();

        urlSteps.open();

        steps.onPriceReportPage().tableRows().get(0).competitorColumn().maxPrice().should(hasText(
                formatNumber(price)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст тултипа по ховеру на «Конкуренты - мин. цена без скидки»")
    public void shouldSeeCompetitorMinPriceTooltipText() {
        int minPrice = getRandomBetween(1000000, 2000000);
        int minPriceDiscount = getRandomBetween(1000000, 2000000);
        int maxTradeIn = getRandomBetween(10000, 20000);
        int maxDiscount = getRandomBetween(10000, 20000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setCompetitorMinPrice(minPrice)
                                                .setCompetitorMinPriceDiscount(minPriceDiscount)
                                                .setMaxTradeIn(maxTradeIn)
                                                .setMaxDiscount(maxDiscount)
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).competitorColumn().minPrice().hover();

        steps.onPriceReportPage().pricePopup().waitUntil(isDisplayed()).should(hasText(format(
                PRICE_POPUP_TEMPLATE,
                formatNumber(minPrice), formatNumber(minPriceDiscount), formatNumber(maxTradeIn), formatNumber(maxDiscount)
        )));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст тултипа по ховеру на «Конкуренты - мин. цена со скидкой»")
    public void shouldSeeCompetitorMinPriceDiscountTooltipText() {
        int minPrice = getRandomBetween(1000000, 2000000);
        int minPriceDiscount = getRandomBetween(1000000, 2000000);
        int maxTradeIn = getRandomBetween(10000, 20000);
        int maxDiscount = getRandomBetween(10000, 20000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setCompetitorMinPrice(minPrice)
                                                .setCompetitorMinPriceDiscount(minPriceDiscount)
                                                .setMaxTradeIn(maxTradeIn)
                                                .setMaxDiscount(maxDiscount)
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).competitorColumn().minPriceDiscount().hover();

        steps.onPriceReportPage().pricePopup().waitUntil(isDisplayed()).should(hasText(format(
                PRICE_POPUP_TEMPLATE,
                formatNumber(minPrice), formatNumber(minPriceDiscount), formatNumber(maxTradeIn), formatNumber(maxDiscount)
        )));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на дилера из тултипа по ховеру на «Конкуренты - мин. цена со скидкой»")
    public void shouldGoToDealerFromMinPriceDiscountTooltipText() {
        int dealerId = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setDealerIdMaxDiscount(dealerId)
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).competitorColumn().minPriceDiscount().hover();

        steps.onPriceReportPage().pricePopup().button(THIS_ONE).click();
        steps.switchToNextTab();
        urlSteps.desktopURI().path(DILER).path(CARS).path(ALL).path(String.valueOf(dealerId)).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на дилера из тултипа по ховеру на «Конкуренты - мин. цена»")
    public void shouldGoToDealerFromMinPriceTooltipText() {
        int dealerId = getRandomBetween(1000000, 2000000);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setDealerIdMaxDiscount(dealerId)
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).competitorColumn().minPrice().hover();

        steps.onPriceReportPage().pricePopup().button(THIS_ONE).click();
        steps.switchToNextTab();
        urlSteps.desktopURI().path(DILER).path(CARS).path(ALL).path(String.valueOf(dealerId)).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст тултипа по ховеру на «Конкуренты - в наличии/под заказ»")
    public void shouldSeeCompetitorAvailableTooltipText() {
        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).competitorColumn().availability().hover();

        steps.onPriceReportPage().hoveredTooltip().waitUntil(isDisplayed()).should(hasText("К списку автомобилей"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на список объявлений по клику на «Конкуренты - в наличии/под заказ»")
    public void shouldGoToOffersList() {
        String markId = "BMW";
        String modelId = "5ER";
        String configurationId = "22213464";
        String superGenId = "22212692";
        String techParamId = "22218461";
        String bodyType = "SEDAN";
        String transmission = "ROBOT";
        String drive = "FORWARD_CONTROL";
        String complectation = "520d";
        int year = getRandomBetween(2000, 2020);

        mockRule.setStubs(
                stub().withGetDeepEquals(DEALER_REPORT_PRICE_GROUPS)
                        .withResponseBody(
                                dealerPriceGroups().setPriceGroups(
                                        dealerPriceGroup()
                                                .setMarkId(markId)
                                                .setModelId(modelId)
                                                .setConfigurationId(configurationId)
                                                .setSuperGenId(superGenId)
                                                .setTechParamId(techParamId)
                                                .setYear(year)
                                                .setBodyType(bodyType)
                                                .setTransmission(transmission)
                                                .setDrive(drive)
                                                .setComplectation(complectation)
                                ).build())
        ).create();

        urlSteps.open();
        steps.onPriceReportPage().tableRows().get(0).competitorColumn().availability().click();

        steps.switchToNextTab();

        urlSteps.desktopURI().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(markId.toLowerCase())
                .path(modelId.toLowerCase()).path(format("%s-%s", superGenId, configurationId)).path(SLASH)
                .addParam(YEAR_FROM, String.valueOf(year)).addParam(YEAR_TO, String.valueOf(year))
                .addParam(BODY_TYPE_GROUP, bodyType).addParam(GEAR_TYPE, drive)
                .addParam(TRANSMISSION, transmission).addParam(TECH_PARAM_ID, techParamId)
                .addParam(COMPLECTATION_NAME, complectation).shouldNotSeeDiff();
    }

}
