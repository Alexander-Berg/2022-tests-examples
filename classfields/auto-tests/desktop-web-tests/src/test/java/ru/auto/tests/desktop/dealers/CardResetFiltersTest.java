package ru.auto.tests.desktop.dealers;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;

@DisplayName("Карточка дилера - Сброс фильтра")
@Feature(DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CardResetFiltersTest {

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
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path("rolf_severo_zapad_avtomobili_s_probegom_moskva/")
                .addParam("body_key", "COUPE")
                .addParam("transmission", "AUTOMATIC")
                .addParam("engine", "GASOLINE")
                .addParam("price_from", "1000")
                .addParam("price_to", "1000000")
                .addParam("year_from", "2014")
                .addParam("year_to", "2016")
                .addParam("color", "EE1D19")
                .addParam("image", "true")
                .addParam("video", "true")
                .addParam("catalog_equipment%5B%5D", "airbag-1")
                .addParam("catalog_equipment%5B%5D", "inch-wheels").open();
        basePageSteps.onListingPage().filter().resetButton().click();
        urlSteps.replaceQuery("").shouldNotSeeDiff();
    }
}
