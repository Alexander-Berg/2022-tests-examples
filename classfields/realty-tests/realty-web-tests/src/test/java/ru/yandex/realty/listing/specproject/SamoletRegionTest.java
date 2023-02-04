package ru.yandex.realty.listing.specproject;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CATALOG;
import static ru.yandex.realty.consts.Pages.MAP;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Лендинг Самолета. Фильтры")
@Link("https://st.yandex-team.ru/VERTISTEST-2188")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SamoletRegionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим смену региона на СПб")
    public void shouldSeeChangeRegionToSpb() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSamoletPage().geoSelectorButton().click();
        basePageSteps.onSamoletPage().geoSelectorPopup().geoSelectorItem("Санкт-Петербург и ЛО").click();
        urlSteps.testing().path(SPB_I_LO).path(SAMOLET).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим смену региона на Москву")
    public void shouldSeeChangeRegionToMsk() {
        urlSteps.testing().path(SPB_I_LO).path(SAMOLET).path(CATALOG).path(MAP).open();
        basePageSteps.onSamoletPage().geoSelectorButton().click();
        basePageSteps.onSamoletPage().geoSelectorPopup().geoSelectorItem("Москва и МО").click();
        urlSteps.testing().path(SAMOLET).path(CATALOG).path(MAP).open();
    }

    @Issue("VERTISTEST-2188")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим закрытие геопопапа по крестику")
    public void shouldSeeCloseGeoPopupByCross() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSamoletPage().geoSelectorButton().click();
        basePageSteps.onSamoletPage().geoSelectorPopup().waitUntil(isDisplayed());
        basePageSteps.onSamoletPage().geoSelectorPopup().closeCross().click();
        basePageSteps.onSamoletPage().geoSelectorPopup().waitUntil(not(isDisplayed()));
    }
}
