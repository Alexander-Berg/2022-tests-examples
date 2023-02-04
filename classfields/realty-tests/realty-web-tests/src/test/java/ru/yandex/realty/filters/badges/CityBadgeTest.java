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
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Гео Фильтры: город")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CityBadgeTest {

    private static final String CITY_NAME = "Санкт-Петербург";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openUrlWithAddress() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Вводим город меняется регион")
    public void shouldSeeAddressInUrl() {
        user.onOffersSearchPage().filters().geoInput().sendKeys(CITY_NAME);
        user.onOffersSearchPage().filters().suggest().waitUntil(hasSize(greaterThan(0))).get(0).click();
        user.loaderWait();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA)
                .toString());
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Бэйджик не появляется после ввода города")
    public void shouldNotSeeBadgeInFilters() {
        user.onOffersSearchPage().filters().geoInput().sendKeys(CITY_NAME);
        user.onOffersSearchPage().filters().suggest().get(0).click();
        user.onOffersSearchPage().filters().badgesCounter().should(not(exists()));
    }
}
