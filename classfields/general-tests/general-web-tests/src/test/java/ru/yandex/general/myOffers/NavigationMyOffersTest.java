package ru.yandex.general.myOffers;

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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.element.MyOfferSnippet.EDIT;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.page.FormPage.SAVE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(MY_OFFERS_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с раздела «Мои объявления» в ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationMyOffersTest {

    private static final String FAVORITES_TITLE = "Избранное";
    private static final String MY_OFFERS_TITLE = "Мои объявления";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        passportSteps.accountWithOffersLogin();
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Мои объявления» с «Избранное» после перехода через сайдбар")
    public void shouldSeeBackToMyOffersFromFavoritesFromSidebar() {
        basePageSteps.onMyOffersPage().lkSidebar().link(FAVORITES_TITLE).click();
        basePageSteps.onFavoritesPage().textH1().waitUntil(hasText(FAVORITES_TITLE));
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onMyOffersPage().textH1().should(hasText(MY_OFFERS_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Мои объявления» в «Избранное» по клику в хедере")
    public void shouldSeeMyOffersToFavoritesFromHeader() {
        basePageSteps.onMyOffersPage().header().linkWithTitle(FAVORITES_TITLE).click();

        basePageSteps.onFavoritesPage().textH1().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Мои объявления» в «Избранное» по клику в прилипшем хедере")
    public void shouldSeeMyOffersToFavoritesFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onMyOffersPage().floatedHeader().linkWithTitle(FAVORITES_TITLE).click();

        basePageSteps.onFavoritesPage().textH1().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку оффера с «Мои объявления»")
    public void shouldSeeMyOffersToOffer() {
        String offerUrl = basePageSteps.onMyOffersPage().snippetFirst().getUrl();
        basePageSteps.onMyOffersPage().snippetFirst().image().click();
        basePageSteps.wait500MS();
        basePageSteps.switchToNextTab();

        basePageSteps.onOfferCardPage().sidebar().price().should(isDisplayed());
        urlSteps.fromUri(offerUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на редактирование оффера с «Мои объявления»")
    public void shouldSeeMyOffersToEditOffer() {
        String editUrl = basePageSteps.onMyOffersPage().snippetFirst().getEditUrl();
        basePageSteps.onMyOffersPage().snippetFirst().hover();
        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).click();

        basePageSteps.onBasePage().h1().should(hasText("Моё объявление"));
        urlSteps.fromUri(editUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются «Мои объявления» после редактирования оффера с «Мои объявления»")
    public void shouldSeeMyOffersAfterSaveEdit() {
        basePageSteps.onMyOffersPage().snippetFirst().hover();
        basePageSteps.onMyOffersPage().snippetFirst().link(EDIT).click();
        basePageSteps.onFormPage().button(SAVE).click();

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

}
