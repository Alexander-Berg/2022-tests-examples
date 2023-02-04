package ru.auto.tests.mobile.favorites;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Notifications.ONE_OFFER_ADDED_TO_FAV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Избранное на карточке под незарегом")
@Feature(FAVORITES)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoritesSaleUnregTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private String saleUrl;

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameter(2)
    public String favoritePostMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser", "desktop/UserFavoritesCarsPost"},
                {TRUCK, "desktop/OfferTrucksUsedUser", "desktop/UserFavoritesTrucksPost"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser", "desktop/UserFavoritesMotoPost"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(stub(saleMock)).create();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        saleUrl = urlSteps.getCurrentUrl();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное на карточке")
    public void shouldAddToFavorites() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().cardActions().addToFavoritesButton());
        authorize();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText(ONE_OFFER_ADDED_TO_FAV));
        basePageSteps.onCardPage().cardActions().deleteFromFavoritesButton().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное в галерее")
    public void shouldAddToFavoritesInGallery() {
        basePageSteps.onCardPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().addToFavoritesButton().click();
        authorize();

        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText(ONE_OFFER_ADDED_TO_FAV));
        basePageSteps.onCardPage().cardActions().deleteFromFavoritesButton().should(isDisplayed());
        basePageSteps.onCardPage().fullScreenGallery().deleteFromFavoritesButton().should(isDisplayed());
    }

    @Step("Авторизуемся")
    private void authorize() {
        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/AuthLoginOrRegisterRedirect"),
                stub("desktop/UserConfirm"),
                stub("desktop/SessionAuthUser"),
                stub(saleMock),
                stub(favoritePostMock)
        ).create();

        basePageSteps.onCardPage().authPopup().switchToAuthPopupFrame();
        basePageSteps.onAuthPage().phoneAuthorize("9111111111");
        basePageSteps.switchToDefaultFrame();

        urlSteps.shouldNotDiffWith(saleUrl);
    }
}
