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
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сохранение поиска на карточке объявления под незарегом")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SavedSearchesSaleUnauthTest {

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

        mockRule.newMock().with("desktop/OfferCarsUsedDealer",
                "desktop/OfferCarsPhones",
                "desktop/UserFavoritesCarsSubscriptionsDealerIdUsedPost",
                "desktop/AuthLoginOrRegisterSearchConfirm").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().subscribeButton().click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сохранение поиска")
    public void shouldSaveSearch() {
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onCardPage().contactsPopup().subscribeButton().waitUntil(hasText("Вы подписаны"));
        basePageSteps.onCardPage().savedSearchesPopup().waitUntil(isDisplayed())
                .should(hasText("FAVORIT MOTORS Юг | Автомобили с пробегом, Все марки автомобилей\nС пробегом\n" +
                        "Электронная почта\nПолучать на почту\nУдалить"));
        basePageSteps.onCardPage().savedSearchesPopup().input("Электронная почта", "test@test.com");
        basePageSteps.onCardPage().savedSearchesPopup().button("Получать на почту").click();
        basePageSteps.onCardPage().savedSearchesPopup().waitUntil(isDisplayed())
                .should(hasText("FAVORIT MOTORS Юг | Автомобили с пробегом, Все марки автомобилей\nС пробегом\n" +
                        "Проверьте почту.\nУдалить"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке на поиск в поп-апе")
    public void shouldClickSavedSearchPopupUrl() {
        basePageSteps.onCardPage().savedSearchesPopup().searchUrl().click();
        urlSteps.testing().path(DILER).path(CARS).path(USED).path("/favorit_motors_ug_moskva/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление поиска")
    public void shouldDeleteSearch() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsDelete").update();

        basePageSteps.onCardPage().savedSearchesPopup().deleteButton().click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск удалён"));
        basePageSteps.onCardPage().savedSearchesPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().contactsPopup().subscribeButton().waitUntil(hasText("Подписаться"));
    }
}