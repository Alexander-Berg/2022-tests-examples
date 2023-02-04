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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.Header.FAVORITE;
import static ru.yandex.general.element.Header.MY_OFFERS;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с карточки оффера на ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationFromOfferCardToLkTest {

    private static final String FAVORITES_TITLE = "Избранное";
    private static final String MY_OFFERS_TITLE = "Мои объявления";

    private String offerCardPath;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        passportSteps.commonAccountLogin();
        offerCardPath = jSoupSteps.getActualOfferCardUrl();
        urlSteps.testing().path(offerCardPath).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с карточки в «Избранное» по клику в хэдере")
    public void shouldSeeCardToFavoritesFromHeader() {
        basePageSteps.onOfferCardPage().header().linkWithTitle(FAVORITE).click();

        basePageSteps.onMyOffersPage().textH1().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с карточки в «Избранное» по клику в прилипшем хэдере")
    public void shouldSeeCardToFavoritesFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onOfferCardPage().floatedHeader().linkWithTitle(FAVORITE).click();

        basePageSteps.onMyOffersPage().textH1().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на карточку с «Избранное» после перехода из хэдера")
    public void shouldSeeBackToCardFromFavoritesFromHeader() {
        String cardTitle = basePageSteps.onOfferCardPage().h1().getText();
        basePageSteps.onOfferCardPage().header().linkWithTitle(FAVORITE).click();
        basePageSteps.onFavoritesPage().textH1().waitUntil(hasText(FAVORITES_TITLE));
        basePageSteps.wait500MS();
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onOfferCardPage().h1().should(hasText(cardTitle));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи с карточки залогином")
    public void shouldSeeGoToForm() {
        basePageSteps.onOfferCardPage().createOffer().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onBasePage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

}
