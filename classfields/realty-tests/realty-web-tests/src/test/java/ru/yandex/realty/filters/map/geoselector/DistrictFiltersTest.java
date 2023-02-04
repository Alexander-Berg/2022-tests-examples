package ru.yandex.realty.filters.map.geoselector;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Location.MOSCOW_OBL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;

@DisplayName("Карта. Фильтры: район")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DistrictFiltersTest {

    private final String TESTING_DISTRICT_NAME = "округ Люберцы";
    private final String TESTING_DISTRICT_ID = "587697";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().filters().geoButtons().spanLink(METRO).click();
        basePageSteps.onMapPage().geoSelectorPopup().waitUntil(isDisplayed());
        basePageSteps.onMapPage().geoSelectorPopup().tab("Район").click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим имя района в урле")
    public void shouldSeeDistrictNameInUrl() {
        basePageSteps.onMapPage().geoSelectorPopup().selectCheckBox(TESTING_DISTRICT_NAME);
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("subLocality", TESTING_DISTRICT_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим бэйджик")
    public void shouldSeeBadge() {
        basePageSteps.onMapPage().geoSelectorPopup().selectCheckBox(TESTING_DISTRICT_NAME);
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges(TESTING_DISTRICT_NAME).should(isDisplayed());
    }
}
