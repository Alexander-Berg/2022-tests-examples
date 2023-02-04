package ru.auto.tests.desktop.savedsearches;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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

import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поп-ап сохраненных поисков")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SavedSearchesPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поп-ап сохраненного поиска под незарегом")
    public void shouldSeeSavedSearchPopupUnauth() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/UserFavoritesAllSubscriptionsEmpty").post();

        urlSteps.testing().open();
        basePageSteps.setWideWindowSize();
        basePageSteps.onListingPage().header().savedSearchesButton().click();
        basePageSteps.onListingPage().headerSavedSearchesPopup().waitUntil(isDisplayed())
                .should(hasText("Поиски\nСохраняйте поиски, чтобы не пропустить новые предложения."));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поп-ап сохраненного поиска под зарегом, у которого есть сохранённые поиски")
    public void shouldSeeSavedSearchPopupAuth() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SessionAuthUser",
                "desktop/UserFavoritesAllSubscriptions").post();

        urlSteps.testing().open();
        basePageSteps.setWideWindowSize();
        basePageSteps.onListingPage().header().savedSearchesButton().click();
        basePageSteps.onListingPage().headerSavedSearchesPopup().waitUntil(isDisplayed())
                .should(hasText("BMW 1 серия\nНовые, до 2 000 000 ₽, от 10 мм\nУведомления на электронную почту\n" +
                        "Получать письма каждые 4 часа\nУдалить"));
    }
}
