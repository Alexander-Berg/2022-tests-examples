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
import static ru.yandex.realty.consts.DeliveryDate.RelativeDate.HALF_YEAR;
import static ru.yandex.realty.consts.DeliveryDate.RelativeDate.ONE_AND_HALF_YEARS;
import static ru.yandex.realty.consts.DeliveryDate.RelativeDate.ONE_YEAR;
import static ru.yandex.realty.consts.DeliveryDate.RelativeDate.THREE_YEARS;
import static ru.yandex.realty.consts.DeliveryDate.RelativeDate.TWO_AND_HALF_YEARS;
import static ru.yandex.realty.consts.DeliveryDate.RelativeDate.TWO_YEARS;
import static ru.yandex.realty.consts.DeliveryDate.getRelativeUrlParam;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.KVARTIRA_NOVOSTROYKI;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedApartmentFiltersDeliveryDateTest {

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
                {HALF_YEAR.getRelativeDate(), getRelativeUrlParam(HALF_YEAR.getInQuarters())},
                {ONE_YEAR.getRelativeDate(), getRelativeUrlParam(ONE_YEAR.getInQuarters())},
                {ONE_AND_HALF_YEARS.getRelativeDate(), getRelativeUrlParam(ONE_AND_HALF_YEARS.getInQuarters())},
                {TWO_YEARS.getRelativeDate(), getRelativeUrlParam(TWO_YEARS.getInQuarters())},
                {TWO_AND_HALF_YEARS.getRelativeDate(), getRelativeUrlParam(TWO_AND_HALF_YEARS.getInQuarters())},
                {THREE_YEARS.getRelativeDate(), getRelativeUrlParam(THREE_YEARS.getInQuarters())},
                {"Сдан", "FINISHED"},
        });
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA_NOVOSTROYKI).open();
        user.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «срок сдачи»")
    public void shouldSeeDeliveryTypeInUrl() {
        user.onOffersSearchPage().extendFilters().checkButton(label);
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam("deliveryDate", expected)
                .queryParam("newFlat", "YES").shouldNotDiffWithWebDriverUrl();
    }
}
