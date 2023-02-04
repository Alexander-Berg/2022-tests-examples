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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.CATALOG;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Лендинг Самолета. Фильтры")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SamoletFiltersDeliveryDateUrlTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KURAU)
    @DisplayName("Параметр «Готовые квартиры»")
    public void shouldSeeDeliveryDateUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).queryParam("deliveryDate", "2_2023").open();
        user.onSamoletPage().searchFilters().button("До 2 квартала 2023").should(isDisplayed());
    }
}
