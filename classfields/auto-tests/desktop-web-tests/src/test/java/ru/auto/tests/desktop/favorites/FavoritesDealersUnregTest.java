package ru.auto.tests.desktop.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление в избранное с листинга объявлений дилера")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FavoritesDealersUnregTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsMercedes",
                "desktop/Salon",
                "desktop/SearchCarsCountDealerId",
                "desktop/SearchCarsDealerId").post();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное с листинга объявлений дилера")
    public void shouldAddToFavoritesFromListing() {
        basePageSteps.onDealerCardPage().getSale(0).hover();
        basePageSteps.onDealerCardPage().getSale(0).toolBar().favoriteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onDealerCardPage().authPopup().waitUntil(isDisplayed());

        mockRule.delete();
        mockRule.newMock().with("desktop/AuthLoginOrRegisterRedirect",
                "desktop/UserConfirm",
                "desktop/SessionAuthUser",
                "desktop/UserFavoritesCarsPost").post();

        basePageSteps.onDealerCardPage().switchToAuthPopupFrame();
        basePageSteps.onDealerCardPage().authPopupFrame().input("Телефон или электронная почта",
                "9111111111");
        basePageSteps.onDealerCardPage().authPopupFrame().button("Продолжить").click();
        basePageSteps.onDealerCardPage().authPopupFrame().input("Код из смс", "1234");
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onDealerCardPage().notifier().waitUntil(isDisplayed()).should(hasText("В избранном 1 предложение" +
                "Перейти в избранное"));
        basePageSteps.onDealerCardPage().authPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().getSale(0).toolBar().favoriteDeleteButton().waitUntil(isDisplayed());
    }
}
