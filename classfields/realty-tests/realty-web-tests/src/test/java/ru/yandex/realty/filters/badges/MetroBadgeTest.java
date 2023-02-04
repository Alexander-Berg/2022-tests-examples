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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Гео Фильтры: метро")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MetroBadgeTest {

    private static final String METRO_NAME = "Академическая";
    private static final String METRO_PATH = "metro-akademicheskaya-1/";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openUrlWithAddress() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA);
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Метро появляется в урле после поиска")
    public void shouldSeeAddressInUrl() {
        urlSteps.open();

        user.onOffersSearchPage().filters().geoInput().sendKeys(METRO_NAME);
        user.onOffersSearchPage().filters().suggest().get(0).click();

        user.onOffersSearchPage().filters().submitButton().click();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA)
                .path(METRO_PATH)
                .toString());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Появляется бэйджик после ввода метро")
    public void shouldSeeBadgeInFilters() {
        urlSteps.open();

        user.onOffersSearchPage().filters().geoInput().sendKeys(METRO_NAME);
        user.onOffersSearchPage().filters().suggest().get(0).click();
        user.onOffersSearchPage().filters().badgesCounter().click();
        user.onOffersSearchPage().filters().badges(METRO_NAME).should(isDisplayed());
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Появляется бэйджик после перехода на урл с метро")
    public void shouldSeeBadgeFromUrl() {
        urlSteps.path(METRO_PATH).open();
        user.onOffersSearchPage().filters().badgesCounter().click();
        user.onOffersSearchPage().filters().badges(METRO_NAME).should(isDisplayed());
    }
}
