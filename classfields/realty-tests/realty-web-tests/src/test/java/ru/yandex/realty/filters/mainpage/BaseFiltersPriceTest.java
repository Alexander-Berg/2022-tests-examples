package ru.yandex.realty.filters.mainpage;

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
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.PRICE_FROM;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;
import static ru.yandex.realty.step.UrlSteps.PRICE_MAX_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.PRICE_MIN_URL_PARAM;
import static ru.yandex.realty.utils.UtilsWeb.getNormalPrice;

@DisplayName("Главная страница. Базовые фильтры.")
@Feature(MAINFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersPriceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «цена от»")
    public void shouldSeePriceMinInUrl() {
        String priceMin = valueOf(getNormalPrice());
        user.onMainPage().filters().price().input(PRICE_FROM).sendKeys(priceMin);
        user.onMainPage().filters().submitButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).queryParam(PRICE_MIN_URL_PARAM, priceMin)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «цена до»")
    public void shouldSeePriceMaxInUrl() {
        String priceMax = valueOf(getNormalPrice());
        user.onMainPage().filters().price().input(TO).sendKeys(priceMax);
        user.onMainPage().filters().submitButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).queryParam(PRICE_MAX_URL_PARAM, priceMax
        ).shouldNotDiffWithWebDriverUrl();
    }
}
