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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.beans.SuggestText;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Гео Фильтры: район")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SubLocalityBadgeTest {

    private static final String SUB_LOCALITY_NAME = "Преображенское";
    private static final String SUB_LOCALITY_API_NAME = "район Преображенское";
    private static final String MOSCOW_RGID = "587795";
    private static final String SUBLOCALITY = "subLocality";

    private SuggestText.Item address;

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
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Район появляется в урле после нажатия кнопки «Найти»")
    public void shouldSeeAddressInUrl() {
        urlSteps.open();

        basePageSteps.onMapPage().filters().geoInput().sendKeys(SUB_LOCALITY_API_NAME);
        basePageSteps.onMapPage().filters().suggest().get(0).click();

        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path("/dist-preobrazhenskoe-193367/").path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Появляется бэйджик после ввода района")
    public void shouldSeeBadgeInFilters() {
        urlSteps.open();

        basePageSteps.onMapPage().filters().geoInput().sendKeys(SUB_LOCALITY_API_NAME);
        basePageSteps.onMapPage().filters().suggest().get(0).click();
        basePageSteps.loaderWait();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges(SUB_LOCALITY_NAME).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Появляется бэйджик после перехода на урл с районом")
    public void shouldSeeBadgeFromUrl() {
        urlSteps.queryParam(SUBLOCALITY, address.getData().getParams().getSubLocality().get(0)).open();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges(SUB_LOCALITY_NAME).should(isDisplayed());
    }


    private SuggestText.Item getFirstSuggest() throws Exception {
        return retrofitApiSteps.suggest(SUB_LOCALITY_API_NAME, MOSCOW_RGID, "SELL", "APARTMENT").get(0);
    }
}
