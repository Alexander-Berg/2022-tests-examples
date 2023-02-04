package ru.yandex.realty.filters.badges;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
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
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Гео Фильтры: несколько фильтров")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MultipleBadgesTest {

    private static final String STREET = "улица Льва Толстого";
    private static final String MOSCOW_RGID = "587795";

    private static final String METRO_NAME = "Академическая";
    private static final String METRO_PATH = "metro-akademicheskaya-1/";
    public static final String METRO_GEO_ID = "metroGeoId";
    public static final String METRO_GEO_ID_VALUE = "20434";
    public static final String STREET_ID = "streetId";
    public static final String STREET_ID_VALUE = "118822";

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
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA);
        address = getFirstSuggest();
    }

    @Description("НЕПОНЯТНА ЛОГИКА С 2 РАЗНЫМИ ТИПА БЕЙДЖЕЙ")
    @Test
    @Owner(KURAU)
    @DisplayName("Адрес и метро появляются в урле после нажатия кнопки «Найти»")
    public void shouldSeeAddressInUrl() {
        urlSteps.open();

        user.onOffersSearchPage().filters().geoInput().sendKeys(STREET);
        user.onOffersSearchPage().filters().suggest().get(0).click();

        user.onOffersSearchPage().filters().geoInput().sendKeys(METRO_NAME);
        user.onOffersSearchPage().filters().suggest().get(0).click();

        user.onOffersSearchPage().filters().submitButton().click();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA)
                .queryParam(METRO_GEO_ID, METRO_GEO_ID_VALUE).queryParam(STREET_ID, STREET_ID_VALUE)
                .toString());
    }

    @Description("НЕПОНЯТНА ЛОГИКА С 2 РАЗНЫМИ ТИПА БЕЙДЖЕЙ")
    @Test
    @Owner(KURAU)
    @DisplayName("Появляются бэйджики после ввода адреса и метро")
    public void shouldSeeBadgeInFilters() {
        urlSteps.open();

        user.onOffersSearchPage().filters().geoInput().sendKeys(STREET);
        user.onOffersSearchPage().filters().suggest().get(0).click();

        user.onOffersSearchPage().filters().geoInput().sendKeys(METRO_NAME);
        user.onOffersSearchPage().filters().suggest().get(0).click();

        user.onOffersSearchPage().filters().badgesCounter().click();
        user.onOffersSearchPage().filters().badges(address.getLabel()).should(isDisplayed());
        user.onOffersSearchPage().filters().badges(METRO_NAME).should(isDisplayed());
    }

    @Description("НЕПОНЯТНА ЛОГИКА С 2 РАЗНЫМИ ТИПА БЕЙДЖЕЙ")
    @Test
    @Owner(KURAU)
    @DisplayName("Появляются бэйджики после перехода на урл")
    public void shouldSeeBadgeFromUrl() {
        urlSteps.path(METRO_PATH)
                .queryParam(METRO_GEO_ID, METRO_GEO_ID_VALUE).queryParam(STREET_ID, STREET_ID_VALUE)
                .open();
        user.onOffersSearchPage().filters().badgesCounter().click();
        user.onOffersSearchPage().filters().badges(address.getLabel()).should(isDisplayed());
    }

    private SuggestText.Item getFirstSuggest() throws Exception {
        return retrofitApiSteps.suggest(STREET, MOSCOW_RGID, "SELL", "APARTMENT").get(0);
    }
}
