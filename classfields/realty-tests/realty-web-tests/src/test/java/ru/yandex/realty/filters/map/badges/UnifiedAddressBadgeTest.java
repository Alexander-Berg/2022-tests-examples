package ru.yandex.realty.filters.map.badges;

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
import ru.yandex.realty.beans.SuggestText;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Гео Фильтры: улица")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class UnifiedAddressBadgeTest {

    private static final String STREET = "Прудовой проезд, 9к1";
    private static final String MOSCOW_RGID = "587795";
    private static final String UNIFIED_ADDRESS_PARAM = "unifiedAddress";

    private SuggestText.Item address;
    private String unifiedAddress;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() throws Exception {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA);
        address = getFirstSuggest();
        unifiedAddress = address.getData().getParams().getUnifiedAddress().get(0);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Адрес появляется в урле")
    public void shouldSeeAddressInUrl() {
        urlSteps.open();

        basePageSteps.onMapPage().filters().geoInput().sendKeys(STREET);
        basePageSteps.onMapPage().filters().suggest().get(0).click();

        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(UNIFIED_ADDRESS_PARAM, unifiedAddress)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Появляется бэйджик после ввода адреса")
    public void shouldSeeBadgeInFilters() {
        urlSteps.open();

        basePageSteps.onMapPage().filters().geoInput().sendKeys(STREET);
        basePageSteps.onMapPage().filters().suggest().get(0).click();
        basePageSteps.loaderWait();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges(address.getLabel()).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Появляется бэйджик после перехода на урл с адресом")
    public void shouldSeeBadgeFromUrl() {
        urlSteps.queryParam(UNIFIED_ADDRESS_PARAM, unifiedAddress).open();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges(address.getLabel()).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Адрес удаляется из урла")
    public void shouldNotSeeAddressInUrl() {
        urlSteps.queryParam(UNIFIED_ADDRESS_PARAM, unifiedAddress).open();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges(STREET).clearGeo().click();
        basePageSteps.loaderWait();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).shouldNotDiffWithWebDriverUrl();
    }

    private SuggestText.Item getFirstSuggest() throws Exception {
        return retrofitApiSteps.suggest(STREET, MOSCOW_RGID, "SELL", "APARTMENT").get(0);
    }
}
