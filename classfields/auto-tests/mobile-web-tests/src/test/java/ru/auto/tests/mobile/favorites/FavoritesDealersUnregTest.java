package ru.auto.tests.mobile.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.ONE_OFFER_ADDED_TO_FAV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Избранное на карточке дилера под незарегом")
@Feature(FAVORITES)
@Story(DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FavoritesDealersUnregTest {

    private static final String SALE = "1076842087-f1e84";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/Salon"),
                stub("desktop/SearchCarsCountDealerId"),
                stub("mobile/SearchCarsAllDealerId")).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное")
    public void shouldAddToFavorites() {
        basePageSteps.onDealerCardPage().getSale(1).hover();
        basePageSteps.onDealerCardPage().getSale(0).addToFavoritesIcon().click();
        authorize();
        urlSteps.shouldNotSeeDiff();

        basePageSteps.scrollDown(300); // подорожник на баг

        basePageSteps.onDealerCardPage().notifier().waitUntil(isDisplayed()).should(hasText(ONE_OFFER_ADDED_TO_FAV));
        basePageSteps.onDealerCardPage().getSale(0).deleteFromFavoritesIcon().should(isDisplayed());
    }

    @Step("Авторизуемся")
    private void authorize() {
        mockRule.delete();
        mockRule.setStubs(stub("desktop/AuthLoginOrRegisterRedirect"),
                stub("desktop/UserConfirm"),
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SessionAuthUser"),
                stub("desktop/Salon"),
                stub("desktop/SearchCarsCountDealerId"),
                stub("mobile/SearchCarsAllDealerId"),
                stub("desktop/UserFavoritesCarsPost")).create();

        basePageSteps.onDealerCardPage().authPopup().switchToAuthPopupFrame();
        basePageSteps.onAuthPage().phoneAuthorize("9111111111");
        basePageSteps.switchToDefaultFrame();
    }
}
