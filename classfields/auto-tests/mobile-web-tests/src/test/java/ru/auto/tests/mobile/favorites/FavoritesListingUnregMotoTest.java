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

import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Notifications.ONE_OFFER_ADDED_TO_FAV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление в избранное в листинге под незарегом")
@Feature(FAVORITES)
@Story(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FavoritesListingUnregMotoTest {

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
        mockRule.setStubs(
                stub("desktop/SearchMotoBreadcrumbsEmpty"),
                stub("mobile/SearchMotoAll")
        ).create();

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL).open();
        basePageSteps.onListingPage().getSale(0).addToFavoritesIcon().waitUntil(isDisplayed()).click();
        authorize();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное")
    public void shouldAddToFavorites() {
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText(ONE_OFFER_ADDED_TO_FAV));
        basePageSteps.onListingPage().getSale(0).deleteFromFavoritesIcon().should(isDisplayed());
    }

    @Step("Авторизуемся")
    private void authorize() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/AuthLoginOrRegisterRedirect"),
                stub("desktop/UserConfirm"),
                stub("desktop/SessionAuthUser"),
                stub("desktop/SearchMotoBreadcrumbsEmpty"),
                stub("mobile/SearchMotoAll"),
                stub("desktop/UserFavoritesMotoPost"),
                stub("desktop/UserFavoritesMotoDelete")
        ).create();

        basePageSteps.onListingPage().authPopup().switchToAuthPopupFrame();
        basePageSteps.onAuthPage().phoneAuthorize("9111111111");
        basePageSteps.switchToDefaultFrame();

        urlSteps.shouldNotSeeDiff();
    }
}
