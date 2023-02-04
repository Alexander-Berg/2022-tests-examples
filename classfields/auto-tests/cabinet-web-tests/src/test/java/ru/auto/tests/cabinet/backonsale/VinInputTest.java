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

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Pages.BACK_ON_SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.CREATION_DATE_FROM;
import static ru.auto.tests.desktop.consts.QueryParams.CREATION_DATE_TO;
import static ru.auto.tests.desktop.consts.QueryParams.SORTING;
import static ru.auto.tests.desktop.consts.QueryParams.VINS;
import static ru.auto.tests.desktop.element.cabinet.backonsale.Filters.VIN_INPUT_PLACEHOLDER;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExample;
import static ru.auto.tests.desktop.mock.MockComeback.comebackRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.COMEBACK;
import static ru.auto.tests.desktop.step.CookieSteps.BACK_ON_SALE_PAGE_COOKIE;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Кабинет дилера. Снова в продаже. Поиск по VIN")
@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.BACK_ON_SALE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class VinInputTest {

    private static final String VIN_1 = "XW8ZZZ16ZHN900632";
    private static final String VIN_2 = "X4XPG98470E945572";

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
        mockRule.setStubs(stub("desktop/SessionAuthDealer"),
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(comebackRequest()))
                        .withResponseBody(comebackExample().getBody())
        ).create();

        cookieSteps.setCookieForBaseDomain(BACK_ON_SALE_PAGE_COOKIE, "1");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KIRILL_PKR)
    @DisplayName("Поиск по VIN")
    public void shouldSearchByVin() {
        basePageSteps.onCabinetOnSaleAgainPage().filters().expansionButton().should(hasText("Все параметры")).click();
        basePageSteps.onCabinetOnSaleAgainPage().filters().input(VIN_INPUT_PLACEHOLDER, VIN_1);
        basePageSteps.onCabinetOnSaleAgainPage().filters().searchButton().click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).ignoreParams(CREATION_DATE_FROM, CREATION_DATE_TO, SORTING)
                .addParam(VINS, VIN_1).shouldNotSeeDiff();

    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KIRILL_PKR)
    @DisplayName("Поиск по двум VIN")
    public void shouldSearchByTwoVins() {
        basePageSteps.onCabinetOnSaleAgainPage().filters().expansionButton().should(hasText("Все параметры")).click();
        basePageSteps.onCabinetOnSaleAgainPage().filters().input(VIN_INPUT_PLACEHOLDER, format("%s,%s", VIN_1, VIN_2));
        basePageSteps.onCabinetOnSaleAgainPage().filters().searchButton().click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).ignoreParams(CREATION_DATE_FROM, CREATION_DATE_TO, SORTING)
                .addParam(VINS, VIN_1).addParam(VINS, VIN_2).shouldNotSeeDiff();
    }

}
