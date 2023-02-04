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
import org.openqa.selenium.Keys;
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

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.element.management.AgencyOfferFilters.PRICE_FROM;
import static ru.yandex.realty.element.management.AgencyOfferFilters.PRICE_TO;
import static ru.yandex.realty.utils.AccountType.AGENT;

@DisplayName("Фильтры агентского оффера.")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PriceAgentFilterTest {

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
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Вводим «Цена от», «до», проверяем что отобразилось в урле")
    public void shouldSeePriceInUrl() {
        String priceFrom = String.valueOf(getRandomShortInt());
        String priceTo = String.valueOf(parseInt(priceFrom) + 1);
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_FROM).sendKeys(priceFrom);
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_TO).sendKeys(priceTo);
        urlSteps.queryParam("price_min", priceFrom).queryParam("price_max", priceTo).ignoreParam("currency")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Вводим «Цена от» больше чем «до» жмем Enter, проверяем что поле «до» стало пустым")
    public void shouldSeeEmptyPriceTo() {
        String price1 = randomNumeric(7);
        String price2 = String.valueOf(parseInt(price1) + 1);
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_TO).sendKeys(price1);
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_FROM).sendKeys(price2 + Keys.ENTER);
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_TO).should(hasValue(""));

    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Вводим «до» меньше чем «Цена от» жмем Enter, проверяем что поле «до» стало пустым")
    public void shouldSeeEmptyPriceFrom() {
        String price1 = randomNumeric(7);
        String price2 = String.valueOf(parseInt(price1) - 1);
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_FROM).sendKeys(price1);
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_TO).sendKeys(price2 + Keys.ENTER);
        managementSteps.onManagementNewPage().agentOfferFilters().input(PRICE_FROM).should(hasValue(""));

    }
}
