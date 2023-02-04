package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.element.management.AgencyOfferFilters.PRICE_FROM;
import static ru.yandex.realty.element.management.AgencyOfferFilters.PRICE_TO;
import static ru.yandex.realty.utils.AccountType.AGENT;

@DisplayName("Фильтры агентского оффера.")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CurrencyAgentFilterTest {

    private static final String RUB_SYMBOL = "\u20BD";
    private static final String USD_SYMBOL = "\u0024";
    private static final String EUR_SYMBOL = "€";
    private static final String PRICE = "1000000";
    private static final String CURRENCY = "currency";
    private static final String PRICE_MIN = "price_min";
    private static final String PRICE_MAX = "price_max";

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

    @Before
    public void before() {
        apiSteps.createVos2Account(account, AGENT);
        managementSteps.setWindowSize(1200, 1600);
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL).create();
        offerBuildingSteps.withPrice(10000, "EUR").create();
        offerBuildingSteps.withPrice(10000, "USD").create();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем валюту «$», проверяем урл")
    public void shouldSeeUsdInUrl() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().agentOfferFilters().button(RUB_SYMBOL).should(not(isEnabled()));
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_TO, PRICE);
        managementSteps.onManagementNewPage().agentOfferFilters().button(RUB_SYMBOL).should(isEnabled());
        managementSteps.onManagementNewPage().agentOfferFilters().select(RUB_SYMBOL, USD_SYMBOL);
        urlSteps.queryParam(CURRENCY, "usd").ignoreParam(PRICE_MAX).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем валюту «€», проверяем урл")
    public void shouldSeedEuroInUrl() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().agentOfferFilters().button(RUB_SYMBOL).should(not(isEnabled()));
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_TO, PRICE);
        managementSteps.onManagementNewPage().agentOfferFilters().button(RUB_SYMBOL).should(isEnabled());
        managementSteps.onManagementNewPage().agentOfferFilters().select(RUB_SYMBOL, EUR_SYMBOL);
        urlSteps.queryParam(CURRENCY, "eur").ignoreParam(PRICE_MAX).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем валюту «\u20BD», проверяем урл")
    public void shouldSeeRubInUrl() {
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(CURRENCY, "eur").open();
        managementSteps.onManagementNewPage().agentOfferFilters().button(EUR_SYMBOL).should(not(isEnabled()));
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_FROM, PRICE);
        managementSteps.onManagementNewPage().agentOfferFilters().button(EUR_SYMBOL).should(isEnabled());
        managementSteps.onManagementNewPage().agentOfferFilters().select(EUR_SYMBOL, RUB_SYMBOL);
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(CURRENCY, "rub").ignoreParam(PRICE_MIN)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Проверяем что оффер с ценой в рублях не отображается при выборе другой валюты")
    public void shouldNotSeeOffer() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_FROM, PRICE);
        managementSteps.onManagementNewPage().agencyOffersList().should(hasSize(greaterThan(0)));
        managementSteps.onManagementNewPage().agentOfferFilters().select(RUB_SYMBOL, USD_SYMBOL);
        managementSteps.onManagementNewPage().agencyOffersList().should(hasSize(0));
    }
}
