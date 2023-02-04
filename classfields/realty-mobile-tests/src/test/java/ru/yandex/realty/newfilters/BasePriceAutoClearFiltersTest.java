package ru.yandex.realty.newfilters;

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
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Базовые фильтры. Сброс цены при неверном значении")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BasePriceAutoClearFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KURAU)
    @DisplayName("Сбрасываем старую цену «от»")
    public void shouldClearFromPrice() {
        int wrongPriceMax = getRandomShortInt();
        int priceMin = wrongPriceMax + 1;

        user.onMobileMainPage().searchFilters().priceMin().sendKeys(valueOf(priceMin));
        user.onMobileMainPage().searchFilters().priceMax().sendKeys(valueOf(wrongPriceMax) + Keys.ENTER);

        user.onMobileMainPage().searchFilters().priceMax().should(hasValue(valueOf(wrongPriceMax)));
        user.onMobileMainPage().searchFilters().priceMin().should(hasValue(""));
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KURAU)
    @DisplayName("Сбрасываем старую цену «до»")
    public void shouldClearToPrice() {
        int priceMax = getRandomShortInt();
        int wrongPriceMin = priceMax + 1;

        user.onMobileMainPage().searchFilters().priceMax().sendKeys(valueOf(priceMax));
        user.onMobileMainPage().searchFilters().priceMin().sendKeys(valueOf(wrongPriceMin) + Keys.ENTER);

        user.onMobileMainPage().searchFilters().priceMin().should(hasValue(valueOf(wrongPriceMin)));
        user.onMobileMainPage().searchFilters().priceMax().should(hasValue(""));
    }
}
