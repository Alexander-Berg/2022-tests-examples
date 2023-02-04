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

import static ru.yandex.realty.consts.Filters.DVUHKOMNATNAYA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CATALOG;
import static ru.yandex.realty.consts.Pages.MAP;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.FROM_SPECIAL;
import static ru.yandex.realty.step.UrlSteps.SAMOLET_VALUE;

@DisplayName("Лендинг Самолета. Фильтры")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SamoletFiltersOnMapTest {

    private static final String SEE_ON_MAP = "Показать на карте";
    private static final String OFFER_PATH = "/verejskaya-41-2871193/";
    private static final String OFFER_PATH_FROM_MAP = "/novoe-vnukovo-2466997/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик «Смотреть на карте»")
    public void shouldSeeMapInUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).open();
        basePageSteps.onSamoletPage().button(SEE_ON_MAP).click();
        urlSteps.path(MAP).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик «Показать списком»")
    public void shouldNotSeeMapInUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).path(MAP).open();
        basePageSteps.onSamoletPage().button("Показать списком").click();
        urlSteps.testing().path(SAMOLET).path(CATALOG).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик «Смотреть на карте» фильтры сохраняются")
    public void shouldSeeFiltersOnMapUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).path(DVUHKOMNATNAYA);
        addFiltersToUrl();
        urlSteps.open();
        basePageSteps.onSamoletPage().button(SEE_ON_MAP).click();
        urlSteps.testing().path(SAMOLET).path(CATALOG).path(MAP).path(DVUHKOMNATNAYA);
        addFiltersToUrl();
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    private void addFiltersToUrl() {
        urlSteps.queryParam("priceMax", "20000000").queryParam("areaMin", "40")
                .queryParam("deliveryDate", "4_2022");
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на оффер")
    public void shouldSeeOfferClick() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).open();
        basePageSteps.onSamoletPage().offer(FIRST).offerLink().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).path(OFFER_PATH)
                .queryParam(FROM_SPECIAL, SAMOLET_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на пин на карте")
    public void shouldSeeMapOfferClick() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).path(MAP).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onSamoletPage().mapOffer(FIRST));
        basePageSteps.onSamoletPage().mapOfferPopup().link().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).path(OFFER_PATH_FROM_MAP)
                .queryParam(FROM_SPECIAL, SAMOLET_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }
}
