package ru.yandex.general.offerCard;

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
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.ExternalLinks.YANDEX_LINK;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.consts.Pages.TOVARI_DLYA_ZHIVOTNIH;
import static ru.yandex.general.consts.Pages.TRANSPORTIROVKA_PERENOSKI;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.SANKT_PETERBURG_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.Header.LOGIN;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.general.mobile.element.Link.HREF;
import static ru.yandex.general.page.BasePage.LOGIN_WITH_YANDEX_ID;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с карточки оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationOfferCardTest {

    private static final String TRANSPORTIROVKA_PERENOSKI_TEXT = "Транспортировка, переноски";
    private static final String ELEKTRONIKA_TEXT = "Электроника";
    private static final String KOMPUTERNAYA_TEHNIKA_TEXT = "Компьютерная техника";

    private String offerCardPath;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        basePageSteps.resize(1920, 1080);
        offerCardPath = jSoupSteps.getActualOfferCardUrl();
        urlSteps.testing().path(offerCardPath).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку в блоке похожих снизу")
    public void shouldSeeGoToBottomSimilarCard() {
        String cardUrl = basePageSteps.onOfferCardPage().similarSnippetFirst().getUrl();
        basePageSteps.onOfferCardPage().similarSnippetFirst().hover().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onOfferCardPage().sidebar().price().should(isDisplayed());
        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(isDisplayed()));
        urlSteps.fromUri(cardUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию в ХК с карточки")
    public void shouldSeeGoToBreadcrumbCategory() {
        basePageSteps.onOfferCardPage().breadcrumbsItem(TRANSPORTIROVKA_PERENOSKI_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText("Транспортировка, переноски в Новосибирске"));
        urlSteps.testing().path(NOVOSIBIRSK).path(TOVARI_DLYA_ZHIVOTNIH).path(TRANSPORTIROVKA_PERENOSKI)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на карточку после перехода на категорию в ХК")
    public void shouldSeeGoBackToCardFromBreadcrumbCategory() {
        String cardTitle = basePageSteps.onOfferCardPage().h1().getText();
        basePageSteps.onOfferCardPage().breadcrumbsItem(TRANSPORTIROVKA_PERENOSKI_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText("Транспортировка, переноски в Новосибирске"));
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onOfferCardPage().h1().should(hasText(cardTitle));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную в сайдбаре с карточки оффера")
    public void shouldSeeGoToHomepageFromSidebar() {
        basePageSteps.onOfferCardPage().sidebarCategories().link("Все объявления").click();

        basePageSteps.onOfferCardPage().h1().should(hasText("Объявления в Новосибирске"));
        urlSteps.testing().path(NOVOSIBIRSK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на родительскую категорию в сайдбаре с карточки оффера")
    public void shouldSeeGoToParentSidebarCategory() {
        basePageSteps.onOfferCardPage().sidebarCategories().link(KOMPUTERNAYA_TEHNIKA_TEXT).click();

        basePageSteps.onOfferCardPage().h1().should(hasText(format("%s в Новосибирске", KOMPUTERNAYA_TEHNIKA_TEXT)));
        urlSteps.testing().path(NOVOSIBIRSK).path(KOMPUTERNAYA_TEHNIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на профиль продавца с карточки")
    public void shouldSeeGoToSellerProfile() {
        String sellerUrl = basePageSteps.onOfferCardPage().sidebar().sellerInfo().link().getAttribute(HREF);
        basePageSteps.onOfferCardPage().sidebar().sellerInfo().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onProfilePage().sidebar().followersCount().should(isDisplayed());
        urlSteps.fromUri(sellerUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого")
    public void shouldSeeGoToHomepageFromLogo() {
        basePageSteps.onOfferCardPage().oLogo().click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Новосибирске"));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого из прилишего хэдера")
    public void shouldSeeGoToHomepageFromLogoFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onOfferCardPage().floatedHeader().oLogo().click();

        basePageSteps.onProfilePage().h1().should(hasText("Объявления в Новосибирске"));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Yandex» через лого")
    public void shouldSeeGoToYandexFromLogo() {
        basePageSteps.onOfferCardPage().yLogo().click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(YANDEX_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Yandex» через лого из прилишего хэдера")
    public void shouldSeeGoToYandexFromLogoFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onOfferCardPage().floatedHeader().yLogo().click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(YANDEX_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи с карточки оффера")
    public void shouldSeeGoToForm() {
        basePageSteps.onOfferCardPage().createOffer().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из футера с карточки оффера")
    public void shouldSeeGoToFooterCategory() {
        basePageSteps.onListingPage().footer().category("Недвижимость").waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().footer().category("Запчасти и аксессуары").waitUntil(isDisplayed()).hover().click();

        basePageSteps.onListingPage().h1().should(hasText("Запчасти и аксессуары в Новосибирске"));
        urlSteps.testing().path(NOVOSIBIRSK).path("/transport-i-zapchasti/").path("/zapchasti-i-aksessuary/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на город из футера с карточки оффера")
    public void shouldSeeGoToFooterCity() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().city("Санкт-Петербург").waitUntil(isDisplayed()).hover().click();

        basePageSteps.onListingPage().h1().should(hasText("Животные и товары для них в Санкт-Петербурге"));
        urlSteps.testing().path(SANKT_PETERBURG).path(TOVARI_DLYA_ZHIVOTNIH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на поиск по адресу с карточки оффера")
    public void shouldSeeSubwaySearch() {
        basePageSteps.onOfferCardPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onOfferCardPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onOfferCardPage().searchBar().suggest().station("Сибирская").click();
        basePageSteps.onOfferCardPage().searchBar().button(SHOW).click();
        basePageSteps.onOfferCardPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(NOVOSIBIRSK).queryParam(METRO_ID_PARAM, "102076").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на поиск по району с карточки оффера")
    public void shouldSeeDistrictSearch() {
        basePageSteps.onOfferCardPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onOfferCardPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onOfferCardPage().searchBar().suggest().checkboxWithLabel("Кировский район").click();
        basePageSteps.onOfferCardPage().button(SHOW).click();
        basePageSteps.onOfferCardPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(NOVOSIBIRSK).queryParam(DISTRICT_ID_PARAM, "102062").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на поиск по адресу с карточки оффера")
    public void shouldSeeAddressSearch() {
        basePageSteps.onOfferCardPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onOfferCardPage().searchBar().fillSearchInput("Бориса Богаткова, 128А");
        basePageSteps.onOfferCardPage().searchBar().suggestItem("Бориса Богаткова, 128А").click();
        basePageSteps.onOfferCardPage().searchBar().button(SHOW).click();
        basePageSteps.onOfferCardPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(NOVOSIBIRSK).queryParam(LATITUDE_PARAM, "55.022644")
                .queryParam(LONGITUDE_PARAM, "82.958977")
                .queryParam(GEO_RADIUS_PARAM, "1000").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на текстовый поиск с карточки оффера")
    public void shouldSeeTextSearch() {
        basePageSteps.onOfferCardPage().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onOfferCardPage().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Новосибирске", ELEKTRONIKA_TEXT.toLowerCase())));
        urlSteps.testing().path(NOVOSIBIRSK).path(ELEKTRONIKA).queryParam(TEXT_PARAM, ELEKTRONIKA_TEXT.toLowerCase())
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на текстовый поиск с карточки оффера из прилипшего хэдера")
    public void shouldSeeTextSearchFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onOfferCardPage().floatedHeader().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onOfferCardPage().floatedHeader().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Новосибирске", ELEKTRONIKA_TEXT.toLowerCase())));
        urlSteps.testing().path(NOVOSIBIRSK).path(ELEKTRONIKA).queryParam(TEXT_PARAM, ELEKTRONIKA_TEXT.toLowerCase())
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на карточку после перехода на категорию в ХК")
    public void shouldSeeGoBackToCardFromTextSearch() {
        String cardTitle = basePageSteps.onOfferCardPage().h1().getText();
        basePageSteps.onOfferCardPage().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onOfferCardPage().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(format("Объявления по запросу «%s» в Новосибирске", ELEKTRONIKA_TEXT.toLowerCase())));
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onOfferCardPage().h1().should(hasText(cardTitle));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Войти» на карточке оффера")
    public void shouldSeeGoToLoginOnOfferCard() {
        basePageSteps.onOfferCardPage().header().link(LOGIN).click();
        basePageSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(format("https://passport.yandex.ru/auth?mode=auth&retpath=%s", urlSteps));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена региона с карточки")
    public void shouldSeeChangeRegion() {
        String cardTitle = basePageSteps.onOfferCardPage().h1().getText();
        basePageSteps.onOfferCardPage().region().click();
        basePageSteps.onOfferCardPage().searchBar().suggest().spanLink("Санкт-Петербург").click();

        basePageSteps.onOfferCardPage().h1().should(hasText(cardTitle));
        basePageSteps.shouldSeeCookie(CLASSIFIED_REGION_ID, SANKT_PETERBURG_ID_VALUE);
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

}
