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

import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Удаление поиска под зарегом")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SavedSearchesListingDeleteAuthTest {

    private static final String MARK = "mercedes";
    private static final String MODEL = "e_klasse";
    private static final String PRICE_TO = "1000000";

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
                "mobile/UserFavoritesAllSubscriptions",
                "mobile/UserFavoritesAllSubscriptionsDelete",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing()
                .path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(Pages.ALL)
                .addParam("price_to", PRICE_TO)
                .open();
        basePageSteps.onListingPage().savedSearch().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение cохранённого поиска в листинге")
    public void shouldSeeSavedSearch() {
        basePageSteps.onListingPage().savedSearch().waitUntil(isDisplayed())
                .should(hasText("Подпишитесь на новые предложения\nMercedes-Benz E-klasse, цена до 1 000 000 ₽\n" +
                        "Сохранён"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке удаления поиска в фабе")
    public void shouldClickDeleteSearchHeaderButton() {
        basePageSteps.onListingPage().fabSubscriptions().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск удалён"));
        basePageSteps.onListingPage().savedSearch().button("Сохранить поиск").waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке удаления поиска в листинге")
    public void shouldClickDeleteSearchListingButton() {
        basePageSteps.onListingPage().savedSearch().button("Сохранён").click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск удалён"));
        basePageSteps.onListingPage().savedSearch().button("Сохранить поиск").waitUntil(isDisplayed());
    }
}
