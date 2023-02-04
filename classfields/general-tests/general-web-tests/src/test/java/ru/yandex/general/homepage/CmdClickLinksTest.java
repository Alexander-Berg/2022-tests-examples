package ru.yandex.general.homepage;

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

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.ExternalLinks.DESKTOP_SUPPORT_LINK;
import static ru.yandex.general.consts.ExternalLinks.FOR_SHOPS_LINK;
import static ru.yandex.general.consts.ExternalLinks.YANDEX_LINK;
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.consts.Pages.USLUGI;
import static ru.yandex.general.consts.QueryParams.REGION_PARAM;
import static ru.yandex.general.element.Header.FOR_SHOPS;
import static ru.yandex.general.element.Header.HELP;
import static ru.yandex.general.element.Header.LOGIN;
import static ru.yandex.general.page.BasePage.LOGIN_WITH_YANDEX_ID;
import static ru.yandex.general.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(HOMEPAGE_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с главной. Открытие ссылок по CMD + Click в новом окне")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class CmdClickLinksTest {

    private static final String ELEKTRONIKA_TEXT = "Электроника";
    private static final String USLUGI_TEXT = "Услуги";
    private static final String ELEKTRONIKA_H1 = "Электроника в Москве";

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
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на категорию из списка главных категорий с главной")
    public void shouldSeeGoToHomeMainCategoryCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onListingPage().homeMainCategories().link(ELEKTRONIKA_TEXT));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(ELEKTRONIKA_H1));
        urlSteps.path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на форму подачи по кнопке «Разместить»")
    public void shouldSeeGoToFormCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onBasePage().createOffer());
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на главную через лого")
    public void shouldSeeGoToMainPageCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onBasePage().oLogo());
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Москве"));
        urlSteps.shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на Yandex через лого")
    public void shouldSeeGoToYandexCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onBasePage().yLogo());
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(YANDEX_LINK).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на категорию в сайдбаре с главной")
    public void shouldSeeGoToSidebarCategoryCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onListingPage().sidebarCategories().link(ELEKTRONIKA_TEXT));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(ELEKTRONIKA_H1));
        urlSteps.path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на категорию из футера с главной")
    public void shouldSeeGoToFooterCategoryCmdClick() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(USLUGI_TEXT).waitUntil(isDisplayed()).hover();
        basePageSteps.cmdClick(basePageSteps.onListingPage().footer().category(USLUGI_TEXT));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(USLUGI_TEXT)));
        urlSteps.path(USLUGI).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход в «Магазинам» по клику в хэдере")
    public void shouldSeeHomePageToForShopsFromHeaderCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onBasePage().header().link(FOR_SHOPS));
        basePageSteps.switchToNextTab();

        basePageSteps.onMyOffersPage().h1().should(hasText("Яндекс.Объявления\nдля магазинов"));
        urlSteps.fromUri(FOR_SHOPS_LINK).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход в «Помощь» по клику в хэдере")
    public void shouldSeeHomePageToHelpFromHeaderCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onBasePage().header().link(HELP));
        basePageSteps.switchToNextTab();

        basePageSteps.onMyOffersPage().h1().should(hasText("Ответы на частые вопросы"));
        urlSteps.fromUri(DESKTOP_SUPPORT_LINK).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на карточку")
    public void shouldSeeGoToOfferCardCmdClick() {
        String offerUrl = basePageSteps.onListingPage().firstSnippet().getUrl();
        basePageSteps.cmdClick(basePageSteps.onListingPage().firstSnippet());
        basePageSteps.switchToNextTab();

        basePageSteps.onOfferCardPage().sidebar().price().should(isDisplayed());
        urlSteps.fromUri(offerUrl).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    private String categoryInMoscow(String categoryName) {
        return format("%s в Москве", categoryName);
    }

}
