package ru.yandex.realty.listing.specproject;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CATALOG;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.utils.UtilsWeb.getNormalPrice;

@DisplayName("Лендинг Самолета. Фильтры")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SamoletFiltersPriceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Цена от»")
    public void shouldSeePriceFromInUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).open();
        String priceFrom = String.valueOf(getNormalPrice());
        user.onSamoletPage().searchFilters().priceFrom().sendKeys(priceFrom);
        urlSteps.queryParam(UrlSteps.PRICE_MIN_URL_PARAM, priceFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Цена до»")
    public void shouldPriceToInUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).open();
        String priceTo = String.valueOf(getNormalPrice());
        user.onSamoletPage().searchFilters().priceTo().sendKeys(priceTo);
        urlSteps.queryParam(UrlSteps.PRICE_MAX_URL_PARAM, priceTo).shouldNotDiffWithWebDriverUrl();
    }
}
