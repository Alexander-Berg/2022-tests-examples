package ru.auto.tests.mobile.savedsearches;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
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
@GuiceModules(MobileEmulationTestsModule.class)
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
                "mobile/SearchCarsCountDealerId",
                "mobile/SearchCarsAllDealerId",
                "mobile/UserFavoritesCarsSubscriptionsDealerIdPost").post();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сохранение поиска")
    public void shouldSaveSearch() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().savedSearch().button("Сохранить поиск"));
        basePageSteps.onDealerCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onDealerCardPage().savedSearch().button("Сохранён").should(isDisplayed());
        basePageSteps.onDealerCardPage().savedSearchesPopup().waitUntil(isDisplayed())
                .should(hasText("Укажите свою почту для получения уведомлений о новых объявлениях\nУкажите почту\n" +
                        "Отправить"));
        basePageSteps.onDealerCardPage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().savedSearchesPopup().closeButton().click();
        basePageSteps.scrollUp(500);
        basePageSteps.onDealerCardPage().header().deleteSearchButton().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сохранение поиска в шапке")
    public void shouldSaveSearchInHeader() {
        basePageSteps.onDealerCardPage().header().saveSearchButton().click();
        basePageSteps.onDealerCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onDealerCardPage().savedSearchesPopup().waitUntil(isDisplayed())
                .should(hasText("Укажите свою почту для получения уведомлений о новых объявлениях\nУкажите почту\n" +
                        "Отправить"));
        basePageSteps.onDealerCardPage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().savedSearchesPopup().closeButton().click();
        basePageSteps.onDealerCardPage().header().deleteSearchButton().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление поиска")
    public void shouldDeleteSearch() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().savedSearch().button("Сохранить поиск"));

        mockRule.with("mobile/UserFavoritesAllSubscriptionsDelete").update();

        basePageSteps.onDealerCardPage().savedSearchesPopup().closeButton().click();
        basePageSteps.onDealerCardPage().savedSearch().button("Сохранён").click();
        basePageSteps.onDealerCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск удалён"));
        basePageSteps.onDealerCardPage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().savedSearchesPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().savedSearch().button("Сохранить поиск").waitUntil(isDisplayed());
        basePageSteps.scrollUp(500);
        basePageSteps.onDealerCardPage().header().saveSearchButton().waitUntil(isDisplayed());
    }
}
