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
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Location.MOSCOW_AND_MO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Owners.TARAS;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;

/**
 * Created by vicdev on 21.04.17.
 */

@DisplayName("Фильтры: район")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DistrictFiltersTest {

    private final String SUBLOCALITY_NAME = "округ Люберцы";
    private final String SUBLOCALITY = "subLocality";
    private final String SUBLOCALITY_ID = "587697";
    private final String DISTRICT_NAME = "Раменки";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openGeoSelectorPopup() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(MOSCOW_AND_MO.getPath()).path(KUPIT).path(KVARTIRA).open();
        user.onOffersSearchPage().filters().geoButtons().spanLink(METRO).click();
        user.onOffersSearchPage().geoSelectorPopup().waitUntil(isDisplayed());
        user.onOffersSearchPage().geoSelectorPopup().tab("Район").click();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Видим имя округа МО в урле")
    public void
    shouldSeeSublocalityNameInUrl() {
        user.onOffersSearchPage().geoSelectorPopup().selectCheckBox(SUBLOCALITY_NAME);
        user.onOffersSearchPage().geoSelectorPopup().submitButton().click();
        user.onOffersSearchPage().filters().submitButton().click();
        urlSteps.queryParam(SUBLOCALITY, SUBLOCALITY_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(TARAS)
    @DisplayName("Видим имя района Москвы в урле")
    public void
    shouldSeeDistrictNameInUrl() {
        user.onOffersSearchPage().geoSelectorPopup().selectCheckBox(DISTRICT_NAME);
        user.onOffersSearchPage().geoSelectorPopup().submitButton().click();
        user.onOffersSearchPage().filters().submitButton().click();
        urlSteps.path("/dist-ramenki-193370/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Видим бэйджик")
    public void shouldSeeBadge() {
        user.onBasePage().geoSelectorPopup().selectCheckBox(SUBLOCALITY_NAME);
        user.onBasePage().geoSelectorPopup().submitButton().click();
        user.onOffersSearchPage().filters().badgesCounter().click();
        user.onOffersSearchPage().filters().badges("округ Люберцы").should(isDisplayed());
    }
}
