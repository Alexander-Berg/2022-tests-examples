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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SEARCHES;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_SEARCHES)
@DisplayName("Скриншот выпадушки с типами нотификаций, поверх сниппета")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class FavoriteSearchNotificationPopupScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setFavorites(favoritesResponse().addSearches(4).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setMoscowCookie();
        compareSteps.resize(375, 812);
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот попапа с типами нотификаций сохраненного поиска")
    public void shouldSeeNotificationText() {
        basePageSteps.onFavoritesPage().firstFavCard().notificationType().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onFavoritesPage().firstFavCard().notificationType().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
