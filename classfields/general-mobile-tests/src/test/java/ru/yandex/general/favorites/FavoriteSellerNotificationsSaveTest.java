package ru.yandex.general.favorites;

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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SELLERS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.element.FavCard.EMAIL_AND_PUSH_TEXT;
import static ru.yandex.general.element.FavCard.EMAIL_TEXT;
import static ru.yandex.general.element.FavCard.PUSH_TEXT;
import static ru.yandex.general.page.PublicProfilePage.LET;
import static ru.yandex.general.page.PublicProfilePage.SUBSCRIBE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_SELLERS)
@DisplayName("Смена оповещения сохраненного продавца")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoriteSellerNotificationsSaveTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String nameInPopup;

    @Parameterized.Parameter(1)
    public String nameInButton;

    @Parameterized.Parameters(name = "Оповещение сохраненного продавца. Смена на «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Отключить", "Выключены"},
                {PUSH_TEXT, PUSH_TEXT},
                {EMAIL_TEXT, EMAIL_TEXT},
                {EMAIL_AND_PUSH_TEXT, EMAIL_AND_PUSH_TEXT}
        });
    }

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();
        basePageSteps.onProfilePage().button(SUBSCRIBE).click();
        basePageSteps.onProfilePage().button(LET).click();
        basePageSteps.onListingPage().popupNotification("Вы успешно подписались!").waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, PROFILES_TAB_VALUE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена оповещения сохраненного продавца")
    public void shouldSeeSearchNotificationChange() {
        basePageSteps.onFavoritesPage().firstFavCard().notificationType().click();
        basePageSteps.onFavoritesPage().popup().modalItem(nameInPopup).waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        basePageSteps.refresh();
        basePageSteps.onFavoritesPage().firstFavCard().notificationType().should(hasText(nameInButton));
    }

}
