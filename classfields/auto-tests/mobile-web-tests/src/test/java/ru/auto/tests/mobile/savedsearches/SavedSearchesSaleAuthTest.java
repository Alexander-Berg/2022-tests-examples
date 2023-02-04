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
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LIKE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SEARCHES;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сохранение поиска на карточке объявления под зарегом")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SavedSearchesSaleAuthTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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

        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedDealer",
                "mobile/UserFavoritesAllSubscriptionsEmpty",
                "mobile/UserFavoritesCarsSubscriptionsDealerIdUsedPost").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().button("Подписаться на объявления"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сохранение поиска")
    public void shouldSaveSearch() {
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onCardPage().savedSearchesPopup().waitUntil(isDisplayed()).should(hasText("Поиск сохранён\n" +
                "На адресsosediuser1@mail.ruбудут отправляться письма со свежими объявлениями.\n" +
                "Посмотреть сохранённые поиски"));
        basePageSteps.onCardPage().savedSearchesPopup().input("Укажите почту").should(not(isDisplayed()));
        basePageSteps.onCardPage().savedSearchesPopup().sendButton().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Посмотреть сохранённые поиски»")
    public void shouldClickShowSearchesUrl() {
        basePageSteps.onCardPage().savedSearchesPopup().showSearchesUrl().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(LIKE).path(SEARCHES).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление поиска")
    public void shouldDeleteSearch() {
        mockRule.with("mobile/UserFavoritesAllSubscriptionsDelete").update();

        basePageSteps.onCardPage().savedSearchesPopup().closeButton().click();
        basePageSteps.onCardPage().button("Вы подписаны").click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск удалён"));
        basePageSteps.onCardPage().button("Подписаться на объявления").waitUntil(isDisplayed());
    }
}
