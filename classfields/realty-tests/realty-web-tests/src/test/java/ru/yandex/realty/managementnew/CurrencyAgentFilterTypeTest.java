package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.core.IsNot.not;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.element.management.AgencyOfferFilters.EUR_SYMBOL;
import static ru.yandex.realty.element.management.AgencyOfferFilters.PRICE_TO;
import static ru.yandex.realty.element.management.AgencyOfferFilters.RUB_SYMBOL;
import static ru.yandex.realty.element.management.AgencyOfferFilters.USD_SYMBOL;
import static ru.yandex.realty.utils.AccountType.AGENT;

@DisplayName("Фильтры агентского оффера. Отображаются только те фильтры в которых есть офферы")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CurrencyAgentFilterTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;


    @Parameterized.Parameter
    public long price;

    @Parameterized.Parameter(1)
    public String currencyType;

    @Parameterized.Parameter(2)
    public String symbol;

    @Parameterized.Parameters(name = "{index} - {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {10000, "EUR", EUR_SYMBOL},
                {10000, "USD", USD_SYMBOL}
        });
    }

    @Before
    public void before() {
        apiSteps.createVos2Account(account, AGENT);
        managementSteps.setWindowSize(1200, 1600);
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL)
                .withPrice(price, currencyType).create();
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Доступна валюта только при наличии оффера")
    public void shouldSeeCurrInUrl() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().agentOfferFilters().button(RUB_SYMBOL).should(not(isEnabled()));
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_TO, "1000000");
        managementSteps.onManagementNewPage().agentOfferFilters().button(RUB_SYMBOL).click();
        managementSteps.onManagementNewPage().agentOfferFilters().selectPopup().item(RUB_SYMBOL).should(isDisplayed());
        managementSteps.onManagementNewPage().agentOfferFilters().selectPopup().item(symbol).should(isDisplayed());
    }
}
