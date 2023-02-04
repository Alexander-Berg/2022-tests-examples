package ru.yandex.general.commonListingCases;

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
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.general.consts.ExternalLinks.DESKTOP_SUPPORT_LINK;
import static ru.yandex.general.consts.ExternalLinks.FOR_SHOPS_LINK;
import static ru.yandex.general.consts.ExternalLinks.TERMS_LINK;
import static ru.yandex.general.consts.ExternalLinks.YANDEX_LINK;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.Header.FOR_SHOPS;
import static ru.yandex.general.element.Header.HELP;
import static ru.yandex.general.element.Header.LOGIN;
import static ru.yandex.general.page.BasePage.LOGIN_WITH_YANDEX_ID;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic("Общие кейсы для страниц с листингами")
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с главной/категории/страницы продавца")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CommonNavigationFromListingsTest {

    private static final String ELEKTRONIKA_TEXT = "Электроника";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "Страница «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Главная", MOSKVA},
                {"Листинг категории", format("%s%s%s", MOSKVA, KOMPUTERNAYA_TEHNIKA, NOUTBUKI).replace("//", "/")},
                {"Профиль продавца", format("%s%s", PROFILE, SELLER_PATH).replace("//", "/")}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.resize(1920, 1080);
        urlSteps.testing().path(path).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи с главной/категории/страницы продавца")
    public void shouldSeeGoToForm() {
        basePageSteps.onBasePage().createOffer().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход в «Магазинам» по клику в хэдере главной/категории/страницы продавца")
    public void shouldSeeHomePageToForShopsFromHeader() {
        basePageSteps.onBasePage().header().link(FOR_SHOPS).click();
        basePageSteps.switchToNextTab();

        basePageSteps.onMyOffersPage().h1().should(hasText("Яндекс.Объявления\nдля магазинов"));
        urlSteps.fromUri(FOR_SHOPS_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход в «Помощь» по клику в хэдере главной/категории/страницы продавца")
    public void shouldSeeHomePageToHelpFromHeader() {
        basePageSteps.onBasePage().header().link(HELP).click();
        basePageSteps.switchToNextTab();

        basePageSteps.onMyOffersPage().h1().should(hasText("Ответы на частые вопросы"));
        urlSteps.fromUri(DESKTOP_SUPPORT_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход в «Помощь» по клику в прилипшем хэдере главной/категории/страницы продавца")
    public void shouldSeeHomePageToHelpFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onBasePage().floatedHeader().link(HELP).click();
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().h1().should(hasText("Ответы на частые вопросы"));
        urlSteps.fromUri(DESKTOP_SUPPORT_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Yandex» через лого c главной/категории/страницы продавца")
    public void shouldSeeGoToYandexFromLogo() {
        basePageSteps.onBasePage().yLogo().click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(YANDEX_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Yandex» через лого из прилишего хэдера c главной/категории/страницы продавца")
    public void shouldSeeGoToYandexFromLogoFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onBasePage().floatedHeader().yLogo().click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(YANDEX_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Войти» c главной/категории/страницы продавца")
    public void shouldSeeGoToLoginOnHomepage() {
        basePageSteps.onListingPage().header().link(LOGIN).click();
        basePageSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(format("https://passport.yandex.ru/auth?mode=auth&retpath=%s", urlSteps));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Помощь» в футере c главной/категории/страницы продавца")
    public void shouldSeeGoToHelpFromFooter() {
        basePageSteps.scrollToBottom();
        basePageSteps.onBasePage().footer().help().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().h1().should(hasText("Ответы на частые вопросы"));
        urlSteps.fromUri(DESKTOP_SUPPORT_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Условия использования» в футере c главной/категории/страницы продавца")
    public void shouldSeeGoToTermsFromFooter() {
        basePageSteps.scrollToBottom();
        basePageSteps.onBasePage().footer().terms().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().h1().should(hasText("Условия использования сервиса «Яндекс.Объявления»"));
        urlSteps.fromUri(TERMS_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из футера с главной/категории/страницы продавца")
    public void shouldSeeGoToFooterCategory() {
        basePageSteps.scrollToBottom();
        basePageSteps.onBasePage().footer().category("Бытовая техника").waitUntil(isDisplayed()).hover().click();

        basePageSteps.onBasePage().h1().should(hasText(format("%s в Москве", "Бытовая техника")));
        urlSteps.testing().path(MOSKVA).path("/bitovaya-tehnika/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на текстовый поиск с главной/категории/страницы продавца")
    public void shouldSeeTextSearch() {
        basePageSteps.onProfilePage().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onProfilePage().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT.toLowerCase())));
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(TEXT_PARAM, ELEKTRONIKA_TEXT.toLowerCase())
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на текстовый поиск из прилипшего хэдера с главной/категории/страницы продавца")
    public void shouldSeeTextSearchFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onProfilePage().floatedHeader().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onProfilePage().floatedHeader().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT.toLowerCase())));
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(TEXT_PARAM, ELEKTRONIKA_TEXT.toLowerCase())
                .shouldNotDiffWithWebDriverUrl();
    }

}
