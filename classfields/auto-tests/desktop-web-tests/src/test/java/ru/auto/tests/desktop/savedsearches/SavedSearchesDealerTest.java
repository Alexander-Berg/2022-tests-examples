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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сохранение поиска на карточке дилера")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SavedSearchesDealerTest {

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsMercedes",
                "desktop/Salon",
                "desktop/SearchCarsCountDealerId",
                "desktop/SearchCarsAllDealerId",
                "desktop/UserFavoritesCarsSubscriptionsDealerIdPost").post();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path(CARS_OFFICIAL_DEALER).path("/").open();
        basePageSteps.onDealerCardPage().filter().saveSearchButton().click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сохранение поиска")
    public void shouldSaveSearch() {
        basePageSteps.onDealerCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onDealerCardPage().filter().saveSearchButton().waitUntil(hasText("Сохранён"));
        basePageSteps.onDealerCardPage().savedSearchesPopup().waitUntil(isDisplayed())
                .should(hasText("Авилон Mercedes-Benz Воздвиженка, Все марки автомобилей\nОфициальный дилер\n" +
                        "Электронная почта\nПолучать на почту\nУдалить"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке на поиск в поп-апе")
    public void shouldClickSavedSearchPopupUrl() {
        basePageSteps.onDealerCardPage().savedSearchesPopup().searchUrl().click();
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление поиска")
    public void shouldDeleteSearch() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsDelete").update();

        basePageSteps.onDealerCardPage().savedSearchesPopup().deleteButton().click();
        basePageSteps.onDealerCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск удалён"));
        basePageSteps.onDealerCardPage().savedSearchesPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().filter().saveSearchButton().waitUntil(hasText("Сохранить поиск"));
    }
}