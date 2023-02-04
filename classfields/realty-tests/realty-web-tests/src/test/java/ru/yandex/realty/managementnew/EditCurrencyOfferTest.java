package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.auto.test.api.realty.offer.create.userid.Price;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;

@Link("https://st.yandex-team.ru/VERTISTEST-1490")
@DisplayName("Редактирование оффера с валютой со страницы личного кабинета")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class EditCurrencyOfferTest {

    private static final String USD_SYMBOL = "$";
    private static final String EUR_SYMBOL = "€";

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
    public void openManagementPage() {
        apiSteps.createVos2Account(account, OWNER);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Нельзя менять цену оффера на «0». Цена в EUR")
    public void shouldNotChangeCurrencyOfferToZeroPrice() {
        long initialPrice = 100L;
        Price price = getDefaultOffer(APARTMENT_SELL).getPrice().withValue(initialPrice).withCurrency("EUR");
        offerBuildingSteps.addNewOffer(account).withBody(getDefaultOffer(APARTMENT_SELL).withPrice(price)).
                withInactive().create().getId();

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().currency().should(hasText(EUR_SYMBOL));
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input()
                .waitUntil(hasValue(valueOf(initialPrice)));
        managementSteps.clearInputByBackSpace(() ->
                managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input());
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().sendKeys("0");
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().waitUntil(hasValue("0"));
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().sendKeys(Keys.ENTER);
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input()
                .should(hasValue(valueOf(initialPrice)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем цену на оффер. Цена в USD")
    public void shouldChangeCurrencyOfferPrice() {
        long initialPrice = 550L;
        long secondPrice = 11000L;
        String initialPriceByMeter = "$ 10 за м²";
        String secondPriceByMeter = "$ 200 за м²";
        Price price = getDefaultOffer(APARTMENT_SELL).getPrice().withValue(initialPrice).withCurrency("USD");
        offerBuildingSteps.addNewOffer(account).withBody(getDefaultOffer(APARTMENT_SELL).withPrice(price)).
                withInactive().create().getId();

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().currency().should(hasText(USD_SYMBOL));
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().byMeter()
                .waitUntil(hasText(initialPriceByMeter));
        managementSteps.clearInputByBackSpace(() ->
                managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input());
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input()
                .sendKeys(valueOf(secondPrice) + Keys.ENTER);
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().byMeter()
                .waitUntil(hasText(secondPriceByMeter));
        apiSteps.waitOfferPrice(account.getId(), secondPrice);
    }
}
