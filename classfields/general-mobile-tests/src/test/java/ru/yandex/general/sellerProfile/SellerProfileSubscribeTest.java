package ru.yandex.general.sellerProfile;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mobile.page.FavoritesPage.PROFILES;
import static ru.yandex.general.mobile.page.ProfilePage.LET;
import static ru.yandex.general.mobile.page.ProfilePage.SET;
import static ru.yandex.general.mobile.page.ProfilePage.SUBSCRIBE;
import static ru.yandex.general.mobile.page.ProfilePage.UNSUBSCRIBE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(PROFILE_FEATURE)
@DisplayName("Страница профиля")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SellerProfileSubscribeTest {

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
        passportSteps.createAccountAndLogin();
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();
        basePageSteps.onProfilePage().button(SUBSCRIBE).click();
        basePageSteps.onProfilePage().popup().button(LET).click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на сохраненные профили по кнопке «Настроить»")
    public void shouldSubscribe() {
        basePageSteps.onProfilePage().button(SET).click();
        basePageSteps.onFavoritesPage().tab(PROFILES).waitUntil(isDisplayed());
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MY).path(FAVORITES)
                .queryParam(TAB_PARAM, PROFILES_TAB_VALUE).toString());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на профиль продавца на странице избранных продавцов")
    public void shouldSeeSavedProfileCardLink() {
        basePageSteps.onProfilePage().button(SET).click();
        basePageSteps.onFavoritesPage().firstFavCard().link().should(hasAttribute(HREF, urlSteps.toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Появляется кнопка «Подписаться» после отписки")
    public void shouldSeeSubscribeButtonAfterUnsubscribe() {
        basePageSteps.onProfilePage().button(UNSUBSCRIBE).click();
        basePageSteps.onProfilePage().popup().button(UNSUBSCRIBE).waitUntil(isDisplayed()).click();
        basePageSteps.onProfilePage().button(SUBSCRIBE).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отписываемся, на странице избранных продавцов нет карточек")
    public void shouldUnsubscribeCheckNoFavorites() {
        basePageSteps.onProfilePage().button(UNSUBSCRIBE).click();
        basePageSteps.onProfilePage().popup().button(UNSUBSCRIBE).waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, PROFILES_TAB_VALUE).open();
        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(0));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отменяем отписку, сохраненный продавец остается")
    public void shouldCancelUnsubscribe() {
        basePageSteps.onProfilePage().button(UNSUBSCRIBE).click();
        basePageSteps.onProfilePage().popup().closeFloatPopup().waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, PROFILES_TAB_VALUE).open();
        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(1));
    }

}
