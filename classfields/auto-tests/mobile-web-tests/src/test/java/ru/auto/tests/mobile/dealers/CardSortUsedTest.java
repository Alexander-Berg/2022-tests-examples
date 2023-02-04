package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.AUTORU_EXCLUSIVE_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.FRESH_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.NAME_ASC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.PRICE_ASC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.PRICE_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.PRICE_PROFITABILITY_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.YEAR_ASC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.YEAR_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка дилера - сортировки")
@Feature(DEALERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardSortUsedTest {

    private static final String DEALER_CODE = "/rolf_ugo_vostok_avtomobili_s_probegom_moskva/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String section;

    @Parameterized.Parameter(1)
    public SortBy sort;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {USED, FRESH_DESC},
                {USED, DATE_DESC},
                {USED, PRICE_ASC},
                {USED, PRICE_DESC},
                {USED, YEAR_ASC},
                {USED, YEAR_DESC},
                {USED, YEAR_DESC},
                {USED, NAME_ASC},
                {USED, AUTORU_EXCLUSIVE_DESC},
                {USED, PRICE_PROFITABILITY_DESC}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(DILER).path(CARS).path(section).path(DEALER_CODE).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сортировка")
    public void shouldSort() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().sortBar()
                .sort(format("По %s", FRESH_DESC.getName())));
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onDealerCardPage().sortPopup().sort(sort.getName()).click();
        basePageSteps.onDealerCardPage().sortPopup().waitUntil(not(isDisplayed()));
        urlSteps.addParam("sort", sort.getAlias()).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().sortBar().sort(format("По %s", sort.getName())).waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}
