package ru.auto.tests.desktop.savedsearches;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Notifications.SEARCH_SAVED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.element.listing.StickySaveSearchPanel.SAVED;
import static ru.auto.tests.desktop.element.listing.StickySaveSearchPanel.SAVE_SEARCH;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг объявлений мото/комтс - сохраненные поиски")
@Feature(SAVE_SEARCHES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SavedSearchesMotoCommerceTest {

    private static final int PXLS_TO_STICKY_SAVE_SEARCH = 1000;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String query;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {MOTORCYCLE, "year_from=2016&year_to=2018&displacement_from=100" +
                        "&displacement_to=500&damage_group=BEATEN&customs_state_group=NOT_CLEARED&price_from=100000" +
                        "&price_to=500000&geo_radius=200"},
                {LCV, "engine_type=DIESEL" +
                        "&transmission=AUTOMATIC&year_from=2006&year_to=2018&customs_state_group=NOT_CLEARED" +
                        "&price_from=100000&price_to=5000000&geo_radius=200"}
        });
    }

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).replaceQuery(query).open();
        basePageSteps.setWideWindowSize(1000);
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сохраняем поиск под незарегом")
    public void shouldSaveSearchUnreg() {
        basePageSteps.onListingPage().filter().saveSearchButton().hover().click();

        basePageSteps.onListingPage().notifier(SEARCH_SAVED).waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().saveSearchButton().waitUntil(isDisplayed())
                .waitUntil(hasText(SAVED)).click();

        basePageSteps.onListingPage().header().savedSearchesButton().click();
        basePageSteps.onListingPage().headerSavedSearchesPopup().getSavedSearch(0).waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сохраняем поиск под незарегом в прилипшей панели")
    public void shouldSaveSearchUnregInStickyPanel() {
        basePageSteps.scrollDown(PXLS_TO_STICKY_SAVE_SEARCH);

        basePageSteps.onListingPage().stickySaveSearchPanel().saveButton().waitUntil(hasText(SAVE_SEARCH)).click();
        basePageSteps.onListingPage().notifier(SEARCH_SAVED).waitUntil(isDisplayed());
        basePageSteps.onListingPage().stickySaveSearchPanel().saveButton().waitUntil(hasText(SAVED));

        basePageSteps.onListingPage().header().savedSearchesButton().click();
        basePageSteps.onListingPage().headerSavedSearchesPopup().getSavedSearch(0).waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();
    }

}
