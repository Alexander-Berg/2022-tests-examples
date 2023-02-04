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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.Pages.TOVARI_DLYA_ZHIVOTNIH;
import static ru.yandex.general.consts.Pages.TRANSPORTIROVKA_PERENOSKI;
import static ru.yandex.general.consts.QueryParams.REGION_PARAM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(LISTING_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация, несколько переходов назад")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class NavigationTwoBackButtonsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        urlSteps.testing().path(NOVOSIBIRSK).path(TOVARI_DLYA_ZHIVOTNIH).path(TRANSPORTIROVKA_PERENOSKI).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с листинга на оффер, затем на оффер из похожих, возвращение на листинг")
    public void shouldSee2BackFromSimilarOffer() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().similarSnippetFirst().hover().click();
        basePageSteps.onOfferCardPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().header().back().click();
        basePageSteps.onOfferCardPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText("Транспортировка, переноски в Новосибирске"));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с листинга на оффер, затем на профиль продавца, возвращение на листинг")
    public void shouldSee2BackFromSellerProfile() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.scrollingToElement(basePageSteps.onOfferCardPage().sellerInfo());
        basePageSteps.onOfferCardPage().sellerInfo().click();
        basePageSteps.onProfilePage().userInfo().waitUntil(isDisplayed());
        basePageSteps.onProfilePage().header().back().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText("Транспортировка, переноски в Новосибирске"));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на оффер, затем на листинг с ХК, затем на оффер, возвращение на первый оффер")
    public void shouldSee2BackFromOfferFromBreadcrumbsListing() {
        String offerUrl = basePageSteps.onListingPage().firstSnippet().getUrl();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().breadcrumbsItem("Животные и товары для них").click();
        basePageSteps.onOfferCardPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().header().back().click();
        basePageSteps.onOfferCardPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().header().back().click();

        urlSteps.fromUri(offerUrl).queryParam(REGION_PARAM, "65").shouldNotDiffWithWebDriverUrl();
    }

}
