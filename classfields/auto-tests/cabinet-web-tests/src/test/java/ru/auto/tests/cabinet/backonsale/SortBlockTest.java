package ru.auto.tests.cabinet.backonsale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.BACK_ON_SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.CREATION_DATE;
import static ru.auto.tests.desktop.consts.QueryParams.CREATION_DATE_FROM;
import static ru.auto.tests.desktop.consts.QueryParams.CREATION_DATE_TO;
import static ru.auto.tests.desktop.consts.QueryParams.LAST_EVENT_DATE_DESC;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.SORTING;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExample;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExampleOneOffer;
import static ru.auto.tests.desktop.mock.MockComeback.comebackRequest;
import static ru.auto.tests.desktop.mock.MockComeback.getFilter;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.COMEBACK;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_BACK_ON_SALE_PLACEHOLDER;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кабинет дилера. Снова в продаже. Блок сортировки")
@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.BACK_ON_SALE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class SortBlockTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(comebackRequest()))
                        .withResponseBody(comebackExample().getBody())
        ).create();

        cookieSteps.setCookieForBaseDomain(IS_SHOWING_BACK_ON_SALE_PLACEHOLDER, "1");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Выбор промежутка дат в календаре")
    public void shouldSelectCalendarDates() {
        mockRule.overwriteStub(4,
                stub().withPostDeepEquals(COMEBACK)
                        .withRequestBody(getJsonObject(
                                comebackRequest().setFilter(getFilter()
                                        .setCreationDateTo(1579122000000L)
                                        .setCreationDateFrom(1577826000000L)
                                        .setSalonGeoIdNull())))
                        .withResponseBody(
                                comebackExampleOneOffer().setStatusSuccessResponse().getBody())
        );

        basePageSteps.onCabinetOnSaleAgainPage().sortsBlock().calendarButton().should(isDisplayed()).hover().click();
        basePageSteps.onCabinetOnSaleAgainPage().calendar().selectPeriod("1 января", "15 января");

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE)
                .addParam(CREATION_DATE_FROM, "2020-01-01")
                .addParam(CREATION_DATE_TO, "2020-01-15")
                .addParam(SORTING, CREATION_DATE)
                .addParam(PAGE, "1")
                .shouldNotSeeDiff();
        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(1));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Сортировка по количеству дней размещения")
    public void shouldSortByDays() {
        mockRule.overwriteStub(4,
                stub().withPostDeepEquals(COMEBACK)
                        .withRequestBody(getJsonObject(
                                comebackRequest().setSorting("LAST_EVENT_DATE_DESC")
                                        .setFilter(getFilter()
                                                .setCreationDateTo(1582750800000L)
                                                .setCreationDateFrom(1580072400000L)
                                                .setSalonGeoIdNull())))
                        .withResponseBody(
                                comebackExampleOneOffer().setSorting("LAST_EVENT_DATE_DESC").setStatusSuccessResponse()
                                        .getBody())
        );

        basePageSteps.onCabinetOnSaleAgainPage().sortsBlock()
                .selectItem("Сортировка", "По количеству дней");
        basePageSteps.onCabinetOnSaleAgainPage().sortsBlock().select("По количеству дней")
                .waitUntil(isDisplayed());

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE)
                .addParam(CREATION_DATE_FROM, "2020-01-27")
                .addParam(CREATION_DATE_TO, "2020-02-26")
                .addParam(SORTING, LAST_EVENT_DATE_DESC)
                .addParam(PAGE, "1")
                .shouldNotSeeDiff();
        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(1));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Чекбокс «Я предыдущий продавец»")
    public void shouldClickIamLastSellerCheckbox() {
        mockRule.overwriteStub(4,
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(
                                comebackRequest().setFilter(getFilter()
                                        .setOnlyLastSeller(true)
                                        .setSalonGeoIdNull())))
                        .withResponseBody(
                                comebackExampleOneOffer().setStatusSuccessResponse().getBody())
        );

        basePageSteps.onCabinetOnSaleAgainPage().sortsBlock().checkbox("Я предыдущий продавец").click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE)
                .addParam(CREATION_DATE_FROM, "2020-01-27")
                .addParam(CREATION_DATE_TO, "2020-02-26")
                .addParam(SORTING, CREATION_DATE)
                .addParam("only_last_seller", "true")
                .addParam(PAGE, "1")
                .shouldNotSeeDiff();
        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(1));
    }

}
