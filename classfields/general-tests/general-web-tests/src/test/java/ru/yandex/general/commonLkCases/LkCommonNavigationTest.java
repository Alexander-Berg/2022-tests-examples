package ru.yandex.general.commonLkCases;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.ExternalLinks.DESKTOP_SUPPORT_LINK;
import static ru.yandex.general.consts.ExternalLinks.FOR_SHOPS_LINK;
import static ru.yandex.general.consts.ExternalLinks.TERMS_LINK;
import static ru.yandex.general.consts.ExternalLinks.YANDEX_LINK;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.Header.FOR_SHOPS;
import static ru.yandex.general.element.Header.HELP;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic("Общие кейсы для ЛК")
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с раздела «Мои объявления» в ЛК")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class LkCommonNavigationTest {

    private static final String ELEKTRONIKA_TEXT = "Электроника";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "Страница «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Статистика", STATS},
                {"Мои объявления", OFFERS},
                {"Избранное", FAVORITES},
                {"Автозагрузка", FEED},
                {"Настройки", CONTACTS}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        passportSteps.accountWithOffersLogin();
        urlSteps.testing().path(MY).path(path).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи со всех страниц ЛК")
    public void shouldSeeGoToForm() {
        basePageSteps.onBasePage().createOffer().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на текстовый поиск со всех страниц ЛК")
    public void shouldSeeTextSearch() {
        basePageSteps.onBasePage().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onBasePage().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT.toLowerCase())));
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(TEXT_PARAM, ELEKTRONIKA_TEXT.toLowerCase())
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на текстовый поиск из прилипшего хэдера со всех страниц ЛК")
    public void shouldSeeTextSearchFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onBasePage().floatedHeader().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onBasePage().floatedHeader().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT.toLowerCase())));
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(TEXT_PARAM, ELEKTRONIKA_TEXT.toLowerCase())
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого со всех страниц ЛК")
    public void shouldSeeGoToHomepageFromLogo() {
        basePageSteps.onBasePage().oLogo().click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Москве"));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого из прилипшего хэдера со всех страниц ЛК")
    public void shouldSeeGoToHomepageFromLogoFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onBasePage().floatedHeader().oLogo().click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Москве"));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на поиск по метро со всех страниц ЛК")
    public void shouldSeeSubwaySearch() {
        basePageSteps.onBasePage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onBasePage().searchBar().suggest().button(METRO).click();
        basePageSteps.onBasePage().searchBar().suggest().station("Павелецкая").click();
        basePageSteps.onBasePage().searchBar().button(SHOW).click();
        basePageSteps.onBasePage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).queryParam(METRO_ID_PARAM, "20475").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на поиск по району со всех страниц ЛК")
    public void shouldSeeDistrictSearch() {
        basePageSteps.onBasePage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onBasePage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onBasePage().searchBar().suggest().checkboxWithLabel("Силино").click();
        basePageSteps.onBasePage().searchBar().button(SHOW).click();
        basePageSteps.onBasePage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).queryParam(DISTRICT_ID_PARAM, "116978").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на поиск по адресу со всех страниц ЛК")
    public void shouldSeeAddressSearch() {
        basePageSteps.onBasePage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onBasePage().searchBar().fillSearchInput("Ленинградский проспект, 80к17");
        basePageSteps.onBasePage().searchBar().suggestItem("Ленинградский проспект, 80к17").click();
        basePageSteps.onBasePage().searchBar().button(SHOW).click();
        basePageSteps.onBasePage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).queryParam(LATITUDE_PARAM, "55.807953")
                .queryParam(LONGITUDE_PARAM, "37.511509")
                .queryParam(GEO_RADIUS_PARAM, "1000").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Помощь» в футере со всех страниц ЛК")
    public void shouldSeeGoToHelpFromFooter() {
        basePageSteps.scrollToBottom();
        basePageSteps.onBasePage().footer().help().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().h1().should(hasText("Ответы на частые вопросы"));
        urlSteps.fromUri(DESKTOP_SUPPORT_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Условия использования» в футере со всех страниц ЛК")
    public void shouldSeeGoToTermsFromFooter() {
        basePageSteps.scrollToBottom();
        basePageSteps.onBasePage().footer().terms().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().h1().should(hasText("Условия использования сервиса «Яндекс.Объявления»"));
        urlSteps.fromUri(TERMS_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Yandex» через лого")
    public void shouldSeeGoToYandexFromLogo() {
        basePageSteps.onBasePage().yLogo().click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(YANDEX_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Yandex» через лого из прилишего хэдера")
    public void shouldSeeGoToYandexFromLogoFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onBasePage().floatedHeader().yLogo().click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(YANDEX_LINK).shouldNotDiffWithWebDriverUrl();
    }

}
