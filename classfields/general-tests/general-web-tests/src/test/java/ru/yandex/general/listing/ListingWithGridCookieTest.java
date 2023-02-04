package ru.yandex.general.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_TYPE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(LISTING_FEATURE)
@Feature(LISTING_TYPE)
@DisplayName("Листинг с кукой «classified_listing_display_type = grid»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ListingWithGridCookieTest {

    private static final String TEXT_SEARCH = "ноутбук apple";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение листинга плиткой на выдаче по категории, с кукой «classified_listing_display_type = grid»")
    public void shouldSeeListListingOnCategory() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().gridListing().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение листинга плиткой на выдаче по текстовому поиску, с кукой «classified_listing_display_type = grid»")
    public void shouldSeeListListingOnTextSearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT_SEARCH).open();
        basePageSteps.onListingPage().gridListing().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение листинга плиткой на главной, с кукой «classified_listing_display_type = grid»")
    public void shouldSeeGridListingOnHomePage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().gridListing().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение листинга плиткой на профиле продавца, с кукой «classified_listing_display_type = grid»")
    public void shouldSeeGridListingOnPublicProfile() {
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();
        basePageSteps.onListingPage().gridListing().should(isDisplayed());
    }

}
