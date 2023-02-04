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
import ru.yandex.general.consts.FavoritesNotificationTypes.FavoriteSearchNotifications;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.FavoritesNotificationTypes.FavoriteSearchNotifications.EMAIL;
import static ru.yandex.general.consts.FavoritesNotificationTypes.FavoriteSearchNotifications.PUSH;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SELLERS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.element.FavCard.EMAIL_AND_PUSH_TEXT;
import static ru.yandex.general.element.FavCard.EMAIL_TEXT;
import static ru.yandex.general.element.FavCard.OFF;
import static ru.yandex.general.element.FavCard.PUSH_TEXT;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSeller.mockBasicSeller;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_SELLERS)
@DisplayName("Текст выбранного типа оповещений")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoriteSellerNotificationsTest {

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
    public MockRule mockRule;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public List<FavoriteSearchNotifications> notification;

    @Parameterized.Parameters(name = "Оповещение сохраненного продавца «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {OFF, asList()},
                {PUSH_TEXT, asList(PUSH)},
                {EMAIL_TEXT, asList(EMAIL)},
                {EMAIL_AND_PUSH_TEXT, asList(EMAIL, PUSH)}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setFavorites(favoritesResponse().sellers(asList(
                        mockBasicSeller().addNotification(notification))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, PROFILES_TAB_VALUE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст выбранного типа оповещений")
    public void shouldSeeNotificationText() {
        basePageSteps.onFavoritesPage().firstFavCard().notificationType().should(hasText(name));
    }

}
