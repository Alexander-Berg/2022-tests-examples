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

import java.io.UnsupportedEncodingException;

import static java.net.URLDecoder.decode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Гео Фильтры: улица")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class UnifiedAddressBadgeTest {

    private static final String STREET = "улица Льва Толстого";
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
    @DisplayName("Адрес появляется в урле после нажатия кнопки «Найти»")
    public void shouldSeeAddressInUrl() throws UnsupportedEncodingException {
        urlSteps.open();

        user.onOffersSearchPage().filters().geoInput().sendKeys(STREET);
        user.onOffersSearchPage().filters().suggest().get(0).click();

        user.onOffersSearchPage().filters().submitButton().click();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .path("/st-ulica-lva-tolstogo-118822/").toString());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Появляется бэйджик после ввода адреса")
    public void shouldSeeBadgeInFilters() {
        urlSteps.open();

        user.onOffersSearchPage().filters().geoInput().sendKeys(STREET);
        user.onOffersSearchPage().filters().suggest().get(0).click();
        user.onOffersSearchPage().filters().badgesCounter().click();
        user.onOffersSearchPage().filters().badges(address.getLabel()).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Появляется бэйджик после перехода на урл с адресом")
    public void shouldSeeBadgeFromUrl() {
        urlSteps.queryParam("unifiedAddress", address.getData().getParams().getUnifiedAddress().get(0))
                .open();
        user.onOffersSearchPage().filters().badgesCounter().click();
        user.onOffersSearchPage().filters().badges(address.getLabel()).should(isDisplayed());
    }

    private SuggestText.Item getFirstSuggest() throws Exception {
        return retrofitApiSteps.suggest(STREET, MOSCOW_RGID, "SELL", "APARTMENT").get(0);
    }
}
