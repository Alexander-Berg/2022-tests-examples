package ru.auto.tests.mobile.savedsearches;

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
import ru.auto.tests.desktop.consts.Pages;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.LIKE;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SEARCHES;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сохраненные поиски")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SavedSearchesTest {

    private static final String MARK = "mercedes";
    private static final String MODEL = "e_klasse";

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                        "desktop/SearchCarsBreadcrumbsRid213",
                        "mobile/UserFavoritesAllSubscriptions",
                        "mobile/UserFavoritesAllSubscriptionsDelete",
                        "mobile/UserFavoritesAllSubscriptionsEmail")
                .post();

        urlSteps.testing().path(LIKE).path(SEARCHES).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сниппет сохраненного поиска")
    public void shouldSeeSavedSearch() {
        basePageSteps.onSearchesPage().getSavedSearch(0).should(hasText("Mercedes-Benz E-klasse\nдо 1 000 000 ₽\n" +
                "Уведомления\nКаждые 4 часа"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ссылка на сохраненный поиск")
    public void shouldClickSavedSearch() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.onSearchesPage().getSavedSearch(0).title().click();
        urlSteps.testing().path(MOSKVA).path(Pages.CARS).path(MARK).path(MODEL).path(Pages.ALL)
                .addParam("geo_radius", "200").addParam("price_to", "1000000")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление сохраненного поиска")
    public void shouldDeleteSavedSearch() {
        basePageSteps.onSearchesPage().getSavedSearch(0).deleteButton().click();
        basePageSteps.onSearchesPage().notifier().waitUntil(hasText("Поиск удалён"));
        basePageSteps.onSearchesPage().savedSearchesList().waitUntil(hasSize(0));
        basePageSteps.onSearchesPage().stub().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Изменение периодичности подписки")
    public void shouldChangeSubscriptionPeriod() {
        basePageSteps.onSearchesPage().getSavedSearch(0).button("Каждые 4 часа").click();
        basePageSteps.onSearchesPage().popup().item("Не получать письма").click();
        basePageSteps.onSearchesPage().getSavedSearch(0).button("Выключены").waitUntil(isDisplayed());
    }
}
