package ru.yandex.realty.filters.badges;

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

import static java.lang.String.format;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Гео Фильтры: район")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SubLocalityBadgeTest {

    private static final String SUB_LOCALITY_NAME = "Преображенское";
    private static final String SUB_LOCALITY_API_NAME = "район Преображенское";
    private static final String MOSCOW_RGID = "587795";

    private SuggestText.Item address;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void openUrlWithAddress() throws Exception {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA);
        address = getFirstSuggest();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Район появляется в урле после нажатия кнопки «Найти»")
    public void shouldSeeAddressInUrl() {
        urlSteps.open();

        user.onOffersSearchPage().filters().geoInput().sendKeys(SUB_LOCALITY_API_NAME);
        user.onOffersSearchPage().filters().suggest().get(0).click();

        user.onOffersSearchPage().filters().submitButton().click();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA)
                .path(format("/dist-preobrazhenskoe-%s/",
                        address.getData().getParams().getSubLocality().get(0)))
                .toString());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Появляется бэйджик после ввода района")
    public void shouldSeeBadgeInFilters() {
        urlSteps.open();

        user.onOffersSearchPage().filters().geoInput().sendKeys(SUB_LOCALITY_API_NAME);
        user.onOffersSearchPage().filters().suggest().get(0).click();
        user.onOffersSearchPage().filters().badgesCounter().click();
        user.onOffersSearchPage().filters().badges(address.getLabel()).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Появляется бэйджик после перехода на урл с районом")
    public void shouldSeeBadgeFromUrl() {
        urlSteps.queryParam("subLocality", address.getData().getParams().getSubLocality().get(0))
                .open();
        user.onOffersSearchPage().filters().badgesCounter().click();
        user.onOffersSearchPage().filters().badges(SUB_LOCALITY_NAME).should(isDisplayed());
    }


    private SuggestText.Item getFirstSuggest() throws Exception {
        return retrofitApiSteps.suggest(SUB_LOCALITY_API_NAME, MOSCOW_RGID, "SELL", "APARTMENT").get(0);
    }
}
