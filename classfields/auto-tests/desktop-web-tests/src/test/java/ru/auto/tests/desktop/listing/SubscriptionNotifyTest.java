package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Нотифайка про подтверждение/удаление подписки")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SubscriptionNotifyTest {

    private static final String SAVED_SUBSCRIPTION_TEXT =
            "Подписка успешно подтверждена. На вашу электронную почту будут отправляться письма со свежими объявлениями.";

    private static final String DELETED_SUBSCRIPTION_TEXT =
            "Подписка поставлена в очередь на удаление. Письма перестанут приходить в ближайшее время.";

    private static final String SAVED_SUBSCRIPTION_ERROR =
            "Произошла ошибка: не удалось подтвердить подписку, попробуйте снова.";

    private static final String DELETED_SUBSCRIPTION_ERROR =
            "Произошла ошибка: удалить подписку не удалось, попробуйте снова.";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Нотифайка про успешное подтверждение подписки")
    public void shouldSeeSavedNotify() {
        urlSteps.testing().path(CARS).path(ALL).addParam("show-searches", "true")
                .addParam("subs_confirm_popup", "true").open();
        basePageSteps.onListingPage().notifier().waitUntil("Нотифайка не появилась", isDisplayed(), 10)
                .should(hasText(SAVED_SUBSCRIPTION_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Нотифайка про неуспешное подтверждение подписки")
    public void shouldSeeNotSavedNotify() {
        urlSteps.testing().path(CARS).path(ALL).addParam("show-searches", "true")
                .addParam("subs_confirm_popup", "false").open();
        basePageSteps.onListingPage().notifier().waitUntil("Нотифайка не появилась", isDisplayed(), 10)
                .should(hasText(SAVED_SUBSCRIPTION_ERROR));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Нотифайка про успешное удаление подписки")
    public void shouldSeeDeletedNotify() {
        urlSteps.testing().path(CARS).path(ALL).addParam("show-searches", "true")
                .addParam("subs_delete_popup", "true").open();
        basePageSteps.onListingPage().notifier().waitUntil("Нотифайка не появилась", isDisplayed(), 10)
                .should(hasText(DELETED_SUBSCRIPTION_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Нотифайка про неуспешное удаление подписки")
    public void shouldSeeNotDeletedNotify() {
        urlSteps.testing().path(CARS).path(ALL).addParam("show-searches", "true")
                .addParam("subs_delete_popup", "false").open();
        basePageSteps.onListingPage().notifier().waitUntil("Нотифайка не появилась", isDisplayed(), 10)
                .should(hasText(DELETED_SUBSCRIPTION_ERROR));
    }
}