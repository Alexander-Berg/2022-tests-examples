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

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.KOMPUTERI;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(LISTING_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с листинга категории. Открытие ссылок по CMD + Click в новом окне")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class CmdClickLinksTest {

    private static final String NOUTBUKI_TEXT = "Ноутбуки";
    private static final String OFFERS_TEXT = "Объявления";
    private static final String KOMPUTERNAYA_TEHNIKA_TEXT = "Компьютерная техника";

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
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(KOMPUTERI).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на дочернюю категорию в сайдбаре с листинга категории")
    public void shouldSeeGoToChildCategoryCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onListingPage().sidebarCategories().link(NOUTBUKI_TEXT));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(NOUTBUKI_TEXT)));
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на главную в сайдбаре с листинга категории")
    public void shouldSeeGoToHomepageFromSidebarCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onListingPage().sidebarCategories().link("Все объявления"));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(OFFERS_TEXT)));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на родительскую категорию в сайдбаре с листинга категории")
    public void shouldSeeGoToParentSidebarCategoryCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onListingPage().sidebarCategories().link(KOMPUTERNAYA_TEHNIKA_TEXT));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERNAYA_TEHNIKA_TEXT)));
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на категорию в ХК с листинга категории")
    public void shouldSeeGoToBreadcrumbCategoryCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onListingPage().breadcrumbsItem(KOMPUTERNAYA_TEHNIKA_TEXT));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERNAYA_TEHNIKA_TEXT)));
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на «Все объявления» в ХК с листинга категории")
    public void shouldSeeGoToBreadcrumbAllOffersCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onListingPage().breadcrumbsItem("Все объявления"));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в России"));
        urlSteps.testing().path(ROSSIYA).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на «Москва» в ХК с листинга категории")
    public void shouldSeeGoToBreadcrumbCityCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onListingPage().breadcrumbsItem("Москва"));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(OFFERS_TEXT)));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на город из футера с листинга категории")
    public void shouldSeeGoToFooterCityCmdClick() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category("Авто").waitUntil(isDisplayed()).hover();
        basePageSteps.cmdClick(basePageSteps.onListingPage().footer().city("Санкт-Петербург"));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText("Компьютеры в Санкт-Петербурге"));
        urlSteps.testing().path(SANKT_PETERBURG).path(KOMPUTERNAYA_TEHNIKA).path(KOMPUTERI)
                .shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    private String categoryInMoscow(String categoryName) {
        return format("%s в Москве", categoryName);
    }

}
