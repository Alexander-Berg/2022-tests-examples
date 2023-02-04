package ru.auto.tests.desktop.savedsearches;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сохранение поиска на группе")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SavedSearchesGroupTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";

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
        mockRule.newMock().with("desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechParam",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/UserFavoritesCarsSubscriptionsGroupPost").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Подписаться на новые»")
    public void shouldClickSubscribeButton() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onGroupPage().subscribeButton().should(isDisplayed()), 0, 100);
        basePageSteps.onGroupPage().subscribeButton().should(isDisplayed()).click();
        basePageSteps.onGroupPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
    }
}