package ru.yandex.realty.filters.offers;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedLocationFiltersDistanceOnFootTest {

    public static final String TRANSPORT = "На транспорте";
    public static final String FOOT = "Пешком";
    public static final String METRO_TRANSPORT = "metroTransport";
    public static final String TIME_TO_METRO = "timeToMetro";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"5 мин", "5"},
                {"15 мин", "15"},
                {"20 мин", "20"},
                {"30 мин", "30"}
        });
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        user.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Время до метро «Пешком»")
    public void shouldSeeOnFootTimeToMetroInUrl() {
        user.scrollToElement(user.onOffersSearchPage().extendFilters().button(FOOT));
        user.onOffersSearchPage().extendFilters().checkButton(FOOT);
        user.onOffersSearchPage().extendFilters().checkButton(label);
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(METRO_TRANSPORT, "ON_FOOT").queryParam(TIME_TO_METRO, expected)
                .shouldNotDiffWithWebDriverUrl();
    }
}
