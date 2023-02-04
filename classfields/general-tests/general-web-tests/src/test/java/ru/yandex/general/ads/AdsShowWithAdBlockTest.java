package ru.yandex.general.ads;

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
import ru.yandex.general.module.GeneralWebModuleWithAdBlock;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static ru.yandex.general.consts.GeneralFeatures.ADS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.LIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADS_FEATURE)
@Feature("Отображение баннеров на разных страницах c AdBlock")
@DisplayName("Отображение баннеров на разных страницах с AdBlock")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModuleWithAdBlock.class)
public class AdsShowWithAdBlockTest {

    private static final String TEXT = "ноутбук apple";

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
        basePageSteps.resize(1920, 1080);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Баннер в блоке навигации на главной")
    public void shouldSeeAdsBannerInNavigationBlockHomepage() {
        urlSteps.testing().path(ROSSIYA).open();
        basePageSteps.findAdblockCookie();

        basePageSteps.onListingPage().adBannerNavigationBlock().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Баннер в блоке навигации на листинге категории, отображение сеткой")
    public void shouldSeeAdsBannerInNavigationBlockCategory() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.findAdblockCookie();

        basePageSteps.onListingPage().adBannerNavigationBlock().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Баннер в сайдбаре на листинге категории, отображение списком")
    public void shouldSeeAdsSidebarCategory() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.findAdblockCookie();

        basePageSteps.onListingPage().adsSidebar().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Баннер в блоке навигации на текстовом поиске, отображение сеткой")
    public void shouldSeeAdsBannerInNavigationBlockTextSearch() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.findAdblockCookie();

        basePageSteps.onListingPage().adBannerNavigationBlock().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Баннер в сайдбаре на листинге категории, отображение списком")
    public void shouldSeeAdsSidebarTextSearch() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.findAdblockCookie();

        basePageSteps.onListingPage().adsSidebar().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("2 баннера на карточке оффера")
    public void shouldSee2AdsBannerOnOfferCard() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        String offerCardPath = jSoupSteps.getActualOfferCardUrl();
        urlSteps.testing().path(offerCardPath).open();
        basePageSteps.findAdblockCookie();

        basePageSteps.onOfferCardPage().adBannerNavigationBlock().should(isDisplayed());
        basePageSteps.onOfferCardPage().adBanner().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Рекламный спиппет на текстовом поиске, отображение списком")
    public void shouldSeeAdsSnippetInTextSearchListListing() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.findAdblockCookie();

        scrollUntilAdsSnippetIsDisplayed();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Рекламный спиппет на текстовом поиске, отображение плиткой")
    public void shouldSeeAdsSnippetInTextSearchGridListing() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.findAdblockCookie();

        scrollUntilAdsSnippetIsDisplayed();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Рекламный спиппет на листинге категории, отображение списком")
    public void shouldSeeAdsSnippetInCategoryListListing() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.findAdblockCookie();

        scrollUntilAdsSnippetIsDisplayed();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Рекламный спиппет на листинге категории, отображение плиткой")
    public void shouldSeeAdsSnippetInCategoryGridListing() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.findAdblockCookie();

        scrollUntilAdsSnippetIsDisplayed();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Рекламный спиппет на листинге категории")
    public void shouldSeeAdsSnippetInHomepageListListing() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.findAdblockCookie();

        scrollUntilAdsSnippetIsDisplayed();
    }

    private void scrollUntilAdsSnippetIsDisplayed() {
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() ->
        {
            basePageSteps.scroll(800);
            basePageSteps.onListingPage().adsSnippet().should(isDisplayed());
        });
    }

}
