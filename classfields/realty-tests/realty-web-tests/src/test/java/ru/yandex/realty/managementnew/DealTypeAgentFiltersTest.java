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
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.test.api.realty.OfferType.APARTMENT_RENT;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.utils.AccountType.AGENT;

@DisplayName("Фильтры агентского оффера.")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealTypeAgentFiltersTest {

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
    public String dealType;

    @Parameterized.Parameter(1)
    public String urlValue;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Продажа", "sell"},
                {"Аренда", "rent"},
        });
    }

    @Before
    public void before() {
        apiSteps.createVos2Account(account, AGENT);
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL).create();
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_RENT).create();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем тип сделки в узком окне, проверяем что отобразилось в урле")
    public void shouldSeeDealTypeInUrl() {
        managementSteps.onManagementNewPage().agentOfferFilters().button(dealType).waitUntil(isDisplayed()).click();
        urlSteps.queryParam("offer_type", urlValue).shouldNotDiffWithWebDriverUrl();
    }
}