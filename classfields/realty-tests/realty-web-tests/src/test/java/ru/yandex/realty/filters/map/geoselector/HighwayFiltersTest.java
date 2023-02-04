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

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.base.GeoSelectorPopup.GeoSelectorPopup.FROM_CITY;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;

@DisplayName("Карта. Фильтры: шоссе")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class HighwayFiltersTest {

    private static final String TEST_DISTANCE = "До 5 км";
    private static final String TEST_HIGHWAY = "Можайское шоссе";
    private static final String TEST_DIRECTION = "Рижское направление";
    private static final String TEST_HIGHWAY_NUMBER = "33";
    private static final List<String> TEST_HIGHWAYS = asList("19", "20", "21");

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().filters().geoButtons().spanLink(METRO).click();
        basePageSteps.onMapPage().geoSelectorPopup().waitUntil(isDisplayed());
        basePageSteps.onMapPage().geoSelectorPopup().tab("Шоссе").click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим  расстояние до Москвы в урле")
    public void shouldSeeMaxDistanceInUrl() {
        basePageSteps.onMapPage().geoSelectorPopup().button(FROM_CITY).click();
        basePageSteps.onMapPage().selectPopup().item(TEST_DISTANCE).click();
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("directionDistanceMax", "5").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим номер шоссе в урле")
    public void shouldSeeDirectionIndexInUrl() {
        basePageSteps.onMapPage().geoSelectorPopup().selectCheckBox(TEST_HIGHWAY);
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("direction", TEST_HIGHWAY_NUMBER).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем направление -> выбираются все шоссе")
    public void shouldSelectAllHighways() {
        basePageSteps.onMapPage().geoSelectorPopup().selectCheckBox(TEST_DIRECTION);
        assertThat(basePageSteps.onMapPage().geoSelectorPopup().checkBoxList().subList(39, 41))
                .allMatch(x -> hasClass(containsString("_checked")).matches(x));
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.loaderWait();
        TEST_HIGHWAYS.forEach(highwayID -> urlSteps.queryParam("direction", highwayID));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }
}
