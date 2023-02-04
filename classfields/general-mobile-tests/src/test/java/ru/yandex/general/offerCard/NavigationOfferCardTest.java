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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.Pages.TOVARI_DLYA_ZHIVOTNIH;
import static ru.yandex.general.consts.Pages.TRANSPORTIROVKA_PERENOSKI;
import static ru.yandex.general.mobile.element.Link.HREF;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(NAVIGATION_FEATURE)
@Feature("Навигация с карточки оффера")
@DisplayName("Навигация с карточки оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class NavigationOfferCardTest {

    private static final String TRANSPORTIROVKA_PERENOSKI_TEXT = "Транспортировка, переноски";

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
        offerCardPath = jSoupSteps.getActualOfferCardUrl();
        urlSteps.testing().path(offerCardPath).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку в блоке похожих снизу")
    public void shouldSeeGoToBottomSimilarCard() {
        String cardUrl = basePageSteps.onOfferCardPage().similarSnippetFirst().getUrl();
        basePageSteps.onOfferCardPage().similarSnippetFirst().hover().click();

        basePageSteps.onOfferCardPage().priceBuyer().should(isDisplayed());
        basePageSteps.onOfferCardPage().fullscreenGallery().should(not(isDisplayed()));
        urlSteps.fromUri(cardUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на карточку после перехода на карточку в блоке похожих снизу")
    public void shouldGoBackFromBottomSimilarCard() {
        String cardTitle = basePageSteps.onOfferCardPage().h1().getText();
        basePageSteps.onOfferCardPage().similarSnippetFirst().hover().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().header().back().click();

        basePageSteps.onOfferCardPage().h1().should(hasText(cardTitle));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку в блоке похожих сверху")
    public void shouldSeeGoToTopSimilarCard() {
        basePageSteps.onOfferCardPage().similarSnippetFirst().hover();
        basePageSteps.scrollToTop();
        String cardUrl = basePageSteps.onOfferCardPage().similarCarouseItems().get(0).getUrl();
        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).click();

        basePageSteps.onOfferCardPage().priceBuyer().should(isDisplayed());
        urlSteps.fromUri(cardUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на карточку после перехода на карточку в блоке похожих сверху")
    public void shouldGoBackFromTopSimilarCard() {
        String cardTitle = basePageSteps.onOfferCardPage().h1().getText();
        basePageSteps.onOfferCardPage().similarSnippetFirst().hover();
        basePageSteps.scrollToTop();
        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().header().back().click();

        basePageSteps.onOfferCardPage().h1().should(hasText(cardTitle));
        urlSteps.shouldNotDiffWithWebDriverUrl();
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
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onOfferCardPage().h1().should(hasText(cardTitle));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на профиль продавца с карточки")
    public void shouldSeeGoToSellerProfile() {
        String sellerUrl = basePageSteps.onOfferCardPage().sellerInfo().link().getAttribute(HREF);
        basePageSteps.scrollingToElement(basePageSteps.onOfferCardPage().sellerInfo());
        basePageSteps.onOfferCardPage().sellerInfo().click();

        basePageSteps.onProfilePage().userInfo().should(isDisplayed());
        urlSteps.fromUri(sellerUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на карточку с профиля продавца")
    public void shouldSeeGoBackToCardFromSellerProfile() {
        String cardTitle = basePageSteps.onOfferCardPage().h1().getText();
        basePageSteps.scrollingToElement(basePageSteps.onOfferCardPage().sellerInfo());
        basePageSteps.onOfferCardPage().sellerInfo().click();
        basePageSteps.onProfilePage().userInfo().waitUntil(isDisplayed());
        basePageSteps.onProfilePage().header().back().click();

        basePageSteps.onOfferCardPage().h1().should(hasText(cardTitle));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого, кнопки «Назад» нет")
    public void shouldSeeGoToHomepageFromLogo() {
        basePageSteps.onListingPage().header().oLogo().click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Новосибирске"));
        basePageSteps.onListingPage().header().back().should(not(isDisplayed()));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет кнопки «Назад» при открытии карточки по прямой ссылке")
    public void shouldNotSeeBackButton() {
        basePageSteps.onOfferCardPage().backButton().should(not(isDisplayed()));
    }

}
