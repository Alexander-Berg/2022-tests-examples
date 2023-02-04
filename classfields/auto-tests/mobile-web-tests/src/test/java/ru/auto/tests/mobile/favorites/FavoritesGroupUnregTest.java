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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Notifications.ONE_OFFER_ADDED_TO_FAV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Избранное на групповой карточке под незарегом")
@Feature(FAVORITES)
@Story(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FavoritesGroupUnregTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";

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
        mockRule.setStubs(stub("mobile/SearchCarsBreadcrumbsMarkModelGroup"),
                stub("mobile/SearchCarsGroupContextGroup"),
                stub("mobile/SearchCarsGroupContextListing")).create();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное")
    public void shouldAddToFavorites() {
        String groupUrl = urlSteps.getCurrentUrl();

        basePageSteps.onGroupPage().getSale(0).addToFavoritesIcon().waitUntil(isDisplayed()).click();

        basePageSteps.onCardPage().authPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().authPopup().iframe()
                .waitUntil(hasAttribute("src", containsString(
                        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                                .addParam("r", encode(groupUrl))
                                .addParam("inModal", "true")
                                .addParam("autoLogin", "true")
                                .addParam("welcomeTitle", "")
                                .toString()
                )));

        authorize();

        urlSteps.shouldNotDiffWith(groupUrl);
        basePageSteps.onGroupPage().notifier().should(isDisplayed()).should(hasText(ONE_OFFER_ADDED_TO_FAV));
        basePageSteps.onGroupPage().getSale(0).deleteFromFavoritesIcon().should(isDisplayed());
    }

    @Step("Авторизуемся")
    private void authorize() {
        mockRule.delete();
        mockRule.setStubs(stub("desktop/AuthLoginOrRegisterRedirect"),
                stub("desktop/UserConfirm"),
                stub("desktop/SessionAuthUser"),
                stub("mobile/SearchCarsBreadcrumbsMarkModelGroup"),
                stub("mobile/SearchCarsGroupContextGroup"),
                stub("mobile/SearchCarsGroupContextListing"),
                stub("desktop/UserFavoritesCarsPost")).create();

        basePageSteps.onGroupPage().authPopup().switchToAuthPopupFrame();
        basePageSteps.onAuthPage().phoneAuthorize("9111111111");
        basePageSteps.switchToDefaultFrame();
    }
}
