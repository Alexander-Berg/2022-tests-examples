package ru.yandex.realty.filters.geoselector;

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
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.base.GeoSelectorPopup.GeoSelectorPopup.FROM_CITY;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;

/**
 * Created by kopitsa on 24.08.17.
 */

@DisplayName("Фильтры: шоссе")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class HighwayFiltersTest {

    private static final String TEST_DISTANCE = "До 5 км";
    private static final String TEST_HIGHWAY = "Калужское шоссе";
    private static final String TEST_DIRECTION = "Рижское направление";
    private static final String TEST_HIGHWAY_PATH = "/shosse-kaluzhskoe/";
    private static final List<String> TEST_HIGHWAYS = asList("19", "20", "21");

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps user;

    @Before
    public void openGeoSelectorPopup() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).open();
        user.onOffersSearchPage().filters().geoButtons().spanLink(METRO).click();
        user.onOffersSearchPage().geoSelectorPopup().waitUntil(isDisplayed());
        user.onOffersSearchPage().geoSelectorPopup().tab("Шоссе").click();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Видим  расстояние до Москвы в урле")
    public void shouldSeeMaxDistanceInUrl() {
        user.onBasePage().geoSelectorPopup().button(FROM_CITY).click();
        user.onBasePage().selectPopup().item(TEST_DISTANCE).click();
        user.onBasePage().geoSelectorPopup().submitButton().click();
        urlSteps.queryParam("directionDistanceMax", "5").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Видим номер шоссе в урле")
    public void shouldSeeDirectionIndexInUrl() {
        user.onBasePage().geoSelectorPopup().selectCheckBox(TEST_HIGHWAY);
        user.onBasePage().geoSelectorPopup().submitButton().click();
        urlSteps.path(TEST_HIGHWAY_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем направление -> выбираются все шоссе")
    public void shouldSelectAllHighways() {
        user.onBasePage().geoSelectorPopup().selectCheckBox(TEST_DIRECTION);
        assertThat(user.onBasePage().geoSelectorPopup().checkBoxList().subList(39, 41))
                .allMatch(x -> hasClass(containsString("_checked")).matches(x));
        user.onBasePage().geoSelectorPopup().submitButton().click();
        user.loaderWait();
        TEST_HIGHWAYS.forEach(highwayID -> urlSteps.queryParam("direction", highwayID));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }
}
