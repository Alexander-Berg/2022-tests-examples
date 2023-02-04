package ru.auto.tests.cabinet.backonsale;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.BACK_ON_SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.CREATION_DATE_FROM;
import static ru.auto.tests.desktop.consts.QueryParams.CREATION_DATE_TO;
import static ru.auto.tests.desktop.consts.QueryParams.SORTING;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExample;
import static ru.auto.tests.desktop.mock.MockComeback.comebackRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.COMEBACK;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_BACK_ON_SALE_PLACEHOLDER;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;

@DisplayName("Кабинет дилера. Снова в продаже. Блок фильтров. Инпуты")
@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.BACK_ON_SALE)
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersInputsTest {

    private static final String VALUE = "100000";

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

    @Parameterized.Parameter
    public String filterGroup;

    @Parameterized.Parameter(1)
    public String filter;

    @Parameterized.Parameter(2)
    public String paramName;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Цена", "от", "price_from"},
                {"Цена", "до", "price_to"},
                {"Пробег", "от", "km_age_from"},
                {"Пробег", "до", "km_age_to"},
        });
    }

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
    @DisplayName("Поиск, фильтруем инпутами")
    public void shouldSearch() {
        basePageSteps.onCabinetOnSaleAgainPage().filters().inputGroup(filterGroup).input(filter).sendKeys(VALUE);
        basePageSteps.onCabinetOnSaleAgainPage().filters().searchButton().click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).ignoreParams(CREATION_DATE_FROM, CREATION_DATE_TO, SORTING)
                .addParam(paramName, VALUE).shouldNotSeeDiff();
    }

}
