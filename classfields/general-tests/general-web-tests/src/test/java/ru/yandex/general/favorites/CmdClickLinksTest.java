package ru.yandex.general.favorites;

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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mobile.page.FavoritesPage.PROFILES;
import static ru.yandex.general.mobile.page.FavoritesPage.SEARCHES;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FAVORITES_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с раздела «Избранное» в ЛК. Открытие ссылок по CMD + Click в новом окне")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class CmdClickLinksTest {

    private static final String NOUTBUKI_H1 = "Ноутбуки в Москве";

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
        urlSteps.testing().path(MY).path(FAVORITES).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на карточку оффера с «Избранное»")
    public void shouldSeeFavoritesToOfferCmdClick() {
        String offerUrl = basePageSteps.onFavoritesPage().firstFavCard().getUrl();
        basePageSteps.cmdClick(basePageSteps.onFavoritesPage().firstFavCard());
        basePageSteps.switchToNextTab();

        basePageSteps.onOfferCardPage().sidebar().price().should(isDisplayed());
        urlSteps.fromUri(offerUrl).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на сохраненный поиск с «Избранное»")
    public void shouldSeeFavoritesToSavedSearchCmdClick() {
        basePageSteps.onFavoritesPage().tab(SEARCHES).click();
        basePageSteps.wait500MS();
        String searchUrl = basePageSteps.onFavoritesPage().firstFavCard().getUrl();
        basePageSteps.cmdClick(basePageSteps.onFavoritesPage().firstFavCard());
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(NOUTBUKI_H1));
        urlSteps.fromUri(searchUrl).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на сохраненный профиль с «Избранное»")
    public void shouldSeeFavoritesToSavedProfileCmdClick() {
        basePageSteps.onFavoritesPage().tab(PROFILES).click();
        basePageSteps.wait500MS();
        String profileUrl = basePageSteps.onFavoritesPage().firstFavCard().getUrl();
        basePageSteps.cmdClick(basePageSteps.onFavoritesPage().firstFavCard());
        basePageSteps.switchToNextTab();

        basePageSteps.onProfilePage().sidebar().followersCount().should(isDisplayed());
        urlSteps.fromUri(profileUrl).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

}
