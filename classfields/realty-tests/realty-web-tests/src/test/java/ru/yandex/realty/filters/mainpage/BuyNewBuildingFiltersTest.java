package ru.yandex.realty.filters.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.DeliveryDate.RelativeDate.HALF_YEAR;
import static ru.yandex.realty.consts.DeliveryDate.RelativeDate.ONE_AND_HALF_YEARS;
import static ru.yandex.realty.consts.DeliveryDate.RelativeDate.ONE_YEAR;
import static ru.yandex.realty.consts.DeliveryDate.getRelativeUrlParam;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.DELIVERY_DATE_ITEM;
import static ru.yandex.realty.element.saleads.FiltersBlock.NEWBUILDINGS_BUTTON;

@DisplayName("Главная страница. Фильтр срока сдачи")
@Feature(MAINFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BuyNewBuildingFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String text;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {HALF_YEAR.getRelativeDate(), getRelativeUrlParam(HALF_YEAR.getInQuarters())},
                {ONE_YEAR.getRelativeDate(), getRelativeUrlParam(ONE_YEAR.getInQuarters())},
                {ONE_AND_HALF_YEARS.getRelativeDate(), getRelativeUrlParam(ONE_AND_HALF_YEARS.getInQuarters())},
        });
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Параметр срока сдачи")
    public void shouldSeeNewBuildingDelivery() {
        basePageSteps.onMainPage().filters().selectButton(NEWBUILDINGS_BUTTON);
        basePageSteps.onMainPage().filters().select(DELIVERY_DATE_ITEM, text);
        basePageSteps.onMainPage().filters().submitButton().click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam("from", "index_nb_sites").queryParam("deliveryDate", expected)
                .shouldNotDiffWithWebDriverUrl();
    }
}
