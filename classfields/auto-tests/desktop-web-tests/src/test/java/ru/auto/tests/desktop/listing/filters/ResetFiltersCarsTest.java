package ru.auto.tests.desktop.listing.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;

@DisplayName("Легковые - сброс фильтра")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ResetFiltersCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс фильтра")
    public void shouldResetFilters() {
        urlSteps.testing().path(MOSKVA).path(CARS).path("/audi/").path("/a3/").path(ALL)
                .addParam("top_days", "7")
                .addParam("sort", "cr_date-desc")
                .addParam("body_type_group", "SEDAN")
                .addParam("transmission", "MECHANICAL")
                .addParam("engine_group", "GASOLINE")
                .addParam("gear_type", "FORWARD_CONTROL")
                .addParam("displacement_from", "200")
                .addParam("displacement_to", "200")
                .addParam("year_from", "2017")
                .addParam("year_to", "2018")
                .addParam("km_age_from", "100")
                .addParam("km_age_to", "100000")
                .addParam("price_from", "100000")
                .addParam("price_to", "1000000")
                .open();

        basePageSteps.onListingPage().filter().resetButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}