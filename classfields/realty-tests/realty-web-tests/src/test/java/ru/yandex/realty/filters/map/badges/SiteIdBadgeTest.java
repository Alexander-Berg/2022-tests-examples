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

import static java.lang.String.format;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Гео Фильтры: название ЖК")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SiteIdBadgeTest {

    private static final String SITE_NAME = "алые паруса";
    private static final String MOSCOW_RGID = "587795";

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
    @DisplayName("Название ЖК появляется в урле")
    public void shouldSeeAddressInUrl() {
        urlSteps.open();

        basePageSteps.onMapPage().filters().geoInput().sendKeys(SITE_NAME);
        basePageSteps.onMapPage().filters().suggest().get(0).click();

        basePageSteps.loaderWait();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .path(format("zhk-alye-parusa-%s/", address.getData().getParams().getSiteId().get(0)))
                .path(KARTA).toString());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Появляется бэйджик после ввода названия ЖК")
    public void shouldSeeBadgeInFilters() {
        urlSteps.open();

        basePageSteps.onMapPage().filters().geoInput().sendKeys(SITE_NAME);
        basePageSteps.onMapPage().filters().suggest().get(0).click();
        basePageSteps.loaderWait();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges(address.getLabel()).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Появляется бэйджик после перехода на урл с ЖК")
    public void shouldSeeBadgeFromUrl() {
        urlSteps.queryParam("siteId", address.getData().getParams().getSiteId().get(0))
                .open();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.onMapPage().filters().badges(address.getLabel()).should(isDisplayed());
    }

    private SuggestText.Item getFirstSuggest() throws Exception {
        return retrofitApiSteps.suggest(SITE_NAME, MOSCOW_RGID, "SELL", "APARTMENT").get(0);
    }
}
