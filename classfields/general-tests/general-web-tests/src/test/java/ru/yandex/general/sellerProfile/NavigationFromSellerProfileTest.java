package ru.yandex.general.sellerProfile;

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

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(PROFILE_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с профиля продавца")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationFromSellerProfileTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку с профиля продавца")
    public void shouldSeeGoToOfferCard() {
        String offerUrl = basePageSteps.onProfilePage().firstSnippet().getUrl();
        basePageSteps.onProfilePage().firstSnippet().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onOfferCardPage().sidebar().price().should(isDisplayed());
        urlSteps.fromUri(offerUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на город из футера с профиля продавца")
    public void shouldSeeGoToFooterCity() {
        basePageSteps.scrollToBottom();
        basePageSteps.onProfilePage().footer().category("Авто").hover();
        basePageSteps.onProfilePage().footer().city("Санкт-Петербург").click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Санкт-Петербурге"));
        urlSteps.testing().path(SANKT_PETERBURG).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого")
    public void shouldSeeGoToHomepageFromLogo() {
        basePageSteps.onProfilePage().oLogo().click();

        basePageSteps.onProfilePage().h1().should(hasText("Объявления в Москве"));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого из прилишего хэдера")
    public void shouldSeeGoToHomepageFromLogoFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onProfilePage().floatedHeader().oLogo().click();

        basePageSteps.onProfilePage().h1().should(hasText("Объявления в Москве"));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на поиск по метро с профиля продавца")
    public void shouldSeeSubwaySearch() {
        basePageSteps.onProfilePage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onProfilePage().searchBar().suggest().button(METRO).click();
        basePageSteps.onProfilePage().searchBar().suggest().station("Павелецкая").click();
        basePageSteps.onProfilePage().searchBar().button(SHOW).click();
        basePageSteps.onProfilePage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).queryParam(METRO_ID_PARAM, "20475").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на поиск по району с профиля продавца")
    public void shouldSeeDistrictSearch() {
        basePageSteps.onProfilePage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onProfilePage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onProfilePage().searchBar().suggest().checkboxWithLabel("Силино").click();
        basePageSteps.onProfilePage().searchBar().button(SHOW).click();
        basePageSteps.onProfilePage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).queryParam(DISTRICT_ID_PARAM, "116978").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на поиск по адресу с профиля продавца")
    public void shouldSeeAddressSearch() {
        basePageSteps.onProfilePage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onProfilePage().searchBar().fillSearchInput("Ленинградский проспект, 80к17");
        basePageSteps.onProfilePage().searchBar().suggestItem("Ленинградский проспект, 80к17").click();
        basePageSteps.onProfilePage().searchBar().button(SHOW).click();
        basePageSteps.onProfilePage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).queryParam(LATITUDE_PARAM, "55.807953")
                .queryParam(LONGITUDE_PARAM, "37.511509")
                .queryParam(GEO_RADIUS_PARAM, "1000").shouldNotDiffWithWebDriverUrl();
    }

}
