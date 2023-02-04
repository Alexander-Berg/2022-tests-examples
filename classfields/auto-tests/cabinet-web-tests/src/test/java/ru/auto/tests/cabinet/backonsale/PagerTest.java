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
import static ru.auto.tests.desktop.consts.QueryParams.PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.SORTING;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExample;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExampleOneOffer;
import static ru.auto.tests.desktop.mock.MockComeback.comebackRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.COMEBACK;
import static ru.auto.tests.desktop.mock.beans.comeback.Pagination.pagination;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_BACK_ON_SALE_PLACEHOLDER;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кабинет дилера. Снова в продаже. Пагинация")
@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.BACK_ON_SALE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PagerTest {

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
                        .withRequestBody(getJsonObject(
                                comebackRequest()))
                        .withResponseBody(
                                comebackExample().getBody()),
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(
                                comebackRequest().setPagination(pagination().setPage(2).setPageSize(10))))
                        .withResponseBody(
                                comebackExampleOneOffer().setStatusSuccessResponse().getBody())
        ).create();

        cookieSteps.setCookieForBaseDomain(IS_SHOWING_BACK_ON_SALE_PLACEHOLDER, "1");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).open();
        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(10));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по кнопке «Показать ещё»")
    public void shouldClickShowMoreButton() {
        basePageSteps.onCabinetOnSaleAgainPage().pager().button("Показать ещё").waitUntil(isDisplayed()).click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE)
                .addParam(CREATION_DATE_FROM, "2020-01-27")
                .addParam(CREATION_DATE_TO, "2020-02-26")
                .addParam(SORTING, CREATION_DATE)
                .addParam(PAGE, "2")
                .shouldNotSeeDiff();
        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(11));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Переход на 2-ую страницу листинга")
    public void shouldClickPage2() {
        basePageSteps.onCabinetOnSaleAgainPage().pager().page("2").waitUntil(isDisplayed()).click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE)
                .addParam(CREATION_DATE_FROM, "2020-01-27")
                .addParam(CREATION_DATE_TO, "2020-02-26")
                .addParam(SORTING, CREATION_DATE)
                .addParam(PAGE, "2")
                .shouldNotSeeDiff();
        basePageSteps.onCabinetOnSaleAgainPage().listing().items().waitUntil(hasSize(1));
    }

}
