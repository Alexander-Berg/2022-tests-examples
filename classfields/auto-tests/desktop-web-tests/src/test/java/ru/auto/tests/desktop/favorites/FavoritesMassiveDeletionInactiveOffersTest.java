package ru.auto.tests.desktop.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Массовое удаление офферов из избранного")
@Feature(FAVORITES)
@Story("Массовое удаление офферов из избранного")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FavoritesMassiveDeletionInactiveOffersTest {

    private static final String DELETED_NOTIFICATION = "Удалено из избранного";

    private int favoritesCount;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/UserFavoritesAllWithNotActiveOffers",
                "desktop/UserFavoritesCarsDelete",
                "desktop/UserFavoritesAllNotActiveDelete",
                "desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок удаления неактивных офферов не появляется после первого клика")
    public void shouldNotSeeDeleteNotActiveOffersAfterOneClick() {
        basePageSteps.onMainPage().header().favoritesButton().click();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();

        basePageSteps.onMainPage().favoritesPopup().deleteNotActiveOffers().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст блока удаления неактивных офферов")
    public void shouldSeeDeleteNotActiveOffersText() {
        basePageSteps.onMainPage().header().favoritesButton().click();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText(DELETED_NOTIFICATION));
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();

        basePageSteps.onMainPage().favoritesPopup().deleteNotActiveOffers().should(hasText(
                "Удалить из избранного\nВ избранном 10 завершённых объявлений\nУдалить только это\nУдалить все 10"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем все неактивные офферы")
    public void shouldSeeDeleteAllNotActiveOffers() {
        basePageSteps.onMainPage().header().favoritesButton().click();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText(DELETED_NOTIFICATION));
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().favoritesPopup().deleteNotActiveOffers().button("Удалить все 10").click();

        basePageSteps.onMainPage().notifier().should(hasText(DELETED_NOTIFICATION));
        basePageSteps.onMainPage().favoritesPopup().favoritesList().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем один неактивный оффер категории «Коммерческий» через блок массового удаления")
    public void shouldSeeDeleteOneNotActiveOfferCommercial() {
        basePageSteps.onMainPage().header().favoritesButton().click();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText(DELETED_NOTIFICATION));

        mockRule.with("desktop/UserFavoritesTrucks2Delete").update();
        favoritesCount = basePageSteps.onMainPage().favoritesPopup().favoritesList().size();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().favoritesPopup().deleteNotActiveOffers().button("Удалить только это").click();

        basePageSteps.onMainPage().notifier().should(hasText(DELETED_NOTIFICATION));
        basePageSteps.onMainPage().favoritesPopup().favoritesList().should(hasSize(favoritesCount - 1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем один неактивный оффер категории «Мото» через блок массового удаления")
    public void shouldSeeDeleteOneNotActiveOfferMoto() {
        basePageSteps.onMainPage().header().favoritesButton().click();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText(DELETED_NOTIFICATION));

        mockRule.with("desktop/UserFavoritesMoto2Delete").update();
        favoritesCount = basePageSteps.onMainPage().favoritesPopup().favoritesList().size();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(1).deleteButton().click();
        basePageSteps.onMainPage().favoritesPopup().deleteNotActiveOffers().button("Удалить только это").click();

        basePageSteps.onMainPage().notifier().should(hasText(DELETED_NOTIFICATION));
        basePageSteps.onMainPage().favoritesPopup().favoritesList().should(hasSize(favoritesCount - 1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем один неактивный оффер категории «Авто» через блок массового удаления")
    public void shouldSeeDeleteOneNotActiveOfferCar() {
        basePageSteps.onMainPage().header().favoritesButton().click();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText(DELETED_NOTIFICATION));

        mockRule.with("desktop/UserFavoritesCars2Delete").update();
        favoritesCount = basePageSteps.onMainPage().favoritesPopup().favoritesList().size();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(2).deleteButton().click();
        basePageSteps.onMainPage().favoritesPopup().deleteNotActiveOffers().button("Удалить только это").click();

        basePageSteps.onMainPage().notifier().should(hasText(DELETED_NOTIFICATION));
        basePageSteps.onMainPage().favoritesPopup().favoritesList().should(hasSize(favoritesCount - 1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрываем блок удаления неактивных офферов")
    public void shouldSeeCloseDeleteAllNotActiveOffersBlock() {
        basePageSteps.onMainPage().header().favoritesButton().click();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText(DELETED_NOTIFICATION));
        favoritesCount = basePageSteps.onMainPage().favoritesPopup().favoritesList().size();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().favoritesPopup().deleteNotActiveOffers().close().click();

        basePageSteps.onMainPage().favoritesPopup().deleteNotActiveOffers().should(not(isDisplayed()));
        basePageSteps.onMainPage().favoritesPopup().favoritesList().should(hasSize(favoritesCount));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не отображается блок массового удаления неактивных офферов без неактивных офферов")
    public void shouldNotSeeDeleteAllNotActiveOffersWithoutNotActiveOffers() {
        mockRule.overwriteStub(1, "desktop/UserFavoritesAll");
        basePageSteps.onMainPage().header().favoritesButton().click();
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText(DELETED_NOTIFICATION));
        basePageSteps.onMainPage().favoritesPopup().getFavorite(0).deleteButton().click();

        basePageSteps.onMainPage().favoritesPopup().deleteNotActiveOffers().should(not(isDisplayed()));
    }

}
