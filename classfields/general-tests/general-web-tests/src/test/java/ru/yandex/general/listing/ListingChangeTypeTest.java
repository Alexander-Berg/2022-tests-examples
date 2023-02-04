package ru.yandex.general.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_TYPE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.element.Filters.ACTIVE;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.LIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(LISTING_FEATURE)
@Feature(LISTING_TYPE)
@DisplayName("Смена типа листинга")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ListingChangeTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена типа листинга плиткой -> список")
    public void shouldSeeChangeListingTypeGridToList() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().filters().listListingDisplayType().click();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().listListing().should(isDisplayed());
        basePageSteps.onListingPage().filters().listListingDisplayType().should(hasClass(containsString(ACTIVE)));
        basePageSteps.onListingPage().filters().gridListingDisplayType().should(not(hasClass(containsString(ACTIVE))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена типа листинга список -> плиткой")
    public void shouldSeeChangeListingTypeListToGrid() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().filters().gridListingDisplayType().click();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().gridListing().should(isDisplayed());
        basePageSteps.onListingPage().filters().gridListingDisplayType().should(hasClass(containsString(ACTIVE)));
        basePageSteps.onListingPage().filters().listListingDisplayType().should(not(hasClass(containsString(ACTIVE))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена куки при смене типа листинга плиткой -> список")
    public void shouldSeeChangeListingTypeCookieGridToList() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().filters().listListingDisplayType().click();
        basePageSteps.wait500MS();

        basePageSteps.shouldSeeCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена куки при смене типа листинга список -> плиткой")
    public void shouldSeeChangeListingTypeCookieListToGrid() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().filters().gridListingDisplayType().click();
        basePageSteps.wait500MS();

        basePageSteps.shouldSeeCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
    }

}
