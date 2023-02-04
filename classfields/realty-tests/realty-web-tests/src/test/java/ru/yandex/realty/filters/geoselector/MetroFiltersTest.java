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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Location.MOSCOW_AND_MO;
import static ru.yandex.realty.consts.Owners.JENKL;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;


@DisplayName("Фильтры: метро")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MetroFiltersTest {

    private final String TESTING_METRO_NAME = "Курская";
    private final String TESTING_METRO_PATH = "/metro-kurskaya/";
    private final String TESTING_METRO_PARAM = "20479";
    private final String SECOND_TESTING_METRO_NAME = "Бауманская";
    private final String SECOND_TESTING_METRO_PARAM = "20478";
    private final String TESTING_METRO_LINE_NAME = "Калининская линия";
    private final List<String> TESTING_METRO_LINE_PARAMS =
            newArrayList("20390", "20391", "20392", "20408", "20468", "20476", "20477", "114781");

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps user;

    @Before
    public void openGeoSelectorPopup() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE)).createWithDefaults();
        urlSteps.testing().path(MOSCOW_AND_MO.getPath()).path(KUPIT).path(KVARTIRA).open();
        user.onOffersSearchPage().filters().geoButtons().spanLink(METRO).click();
        user.onOffersSearchPage().geoSelectorPopup().waitUntil(isDisplayed());
        user.onOffersSearchPage().geoSelectorPopup().tab("Метро").click();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Видим название метро в урле")
    public void shouldSeeMetroStationInUrl() {
        user.onBasePage().geoSelectorPopup().metroSuggest().input().sendKeys(TESTING_METRO_NAME);
        user.onBasePage().geoSelectorPopup().metroSuggest().suggestListItem(TESTING_METRO_NAME).click();
        user.onBasePage().geoSelectorPopup().submitButton().click();
        urlSteps.path(TESTING_METRO_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Видим, что два метро переносятся в параметры, а не путь в урле")
    public void shouldSeeTwoMetroStationsInUrl() {
        user.onBasePage().geoSelectorPopup().metroSuggest().input().sendKeys(TESTING_METRO_NAME);
        user.onBasePage().geoSelectorPopup().metroSuggest().suggestListItem(TESTING_METRO_NAME).click();
        user.onBasePage().geoSelectorPopup().metroSuggest().input().sendKeys(SECOND_TESTING_METRO_NAME);
        user.onBasePage().geoSelectorPopup().metroSuggest().suggestListItem(SECOND_TESTING_METRO_NAME).click();
        user.onBasePage().geoSelectorPopup().submitButton().click();
        urlSteps.queryParam("metroGeoId", TESTING_METRO_PARAM)
                .queryParam("metroGeoId", SECOND_TESTING_METRO_PARAM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Видим линию метро в урле")
    public void shouldSeeMetroLineInUrl() {
        user.onBasePage().geoSelectorPopup().metroLineButton().click();
        user.onBasePage().selectPopup().item(TESTING_METRO_LINE_NAME).click();
        user.onBasePage().geoSelectorPopup().submitButton().click();
        user.loaderWait();
        TESTING_METRO_LINE_PARAMS.forEach(param -> urlSteps.queryParam("metroGeoId", param));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(JENKL)
    @DisplayName("Видим название метро и количество комнат в урле")
    public void shouldSeeMetroStationAndRoomTotalParamInUrl() {
        user.onBasePage().geoSelectorPopup().metroSuggest().input().sendKeys(TESTING_METRO_NAME);
        user.onBasePage().geoSelectorPopup().metroSuggest().suggestListItem(TESTING_METRO_NAME).click();
        user.onBasePage().geoSelectorPopup().submitButton().click();
        user.loaderWait();
        urlSteps.path(TESTING_METRO_PATH).shouldNotDiffWithWebDriverUrl();

        user.onOffersSearchPage().filters().checkButton("1");
        user.onOffersSearchPage().filters().submitButton().click();
        user.loaderWait();
        urlSteps.replacePath("moskva_i_moskovskaya_oblast/kupit/kvartira/odnokomnatnaya/metro-kurskaya/")
                .shouldNotDiffWithWebDriverUrl();
    }
}
