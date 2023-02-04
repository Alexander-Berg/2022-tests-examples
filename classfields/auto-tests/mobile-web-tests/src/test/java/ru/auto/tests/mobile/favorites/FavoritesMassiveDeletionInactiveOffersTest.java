package ru.auto.tests.mobile.favorites;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Notifications.DELETED_FROM_FAV;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.LIKE;
import static ru.auto.tests.desktop.mobile.page.FavoritesPage.DELETE_ONLY_THIS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Массовое удаление офферов из избранного")
@Feature(FAVORITES)
@Story("Массовое удаление офферов из избранного")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FavoritesMassiveDeletionInactiveOffersTest {

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
                "mobile/UserFavoritesAllWithNotActiveOffers",
                "desktop/UserFavoritesCarsDelete",
                "desktop/UserFavoritesAllNotActiveDelete").post();

        urlSteps.testing().path(LIKE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Попап удаления неактивных офферов не появляется после первого клика")
    public void shouldNotSeeDeleteNotActiveOffersAfterOneClick() {
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();

        basePageSteps.onMainPage().popup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа массового удаления неактивных офферов")
    public void shouldSeeDeleteNotActiveOffersText() {
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().notifier().waitUntil(hasText(DELETED_FROM_FAV));
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();

        basePageSteps.onFavoritesPage().popup().should(hasText(
                "Удалить из избранного\nВ избранном 10 завершённых объявлений\nУдалить только это\nУдалить все 10"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем все неактивные офферы")
    public void shouldSeeDeleteAllNotActiveOffers() {
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().notifier().waitUntil(hasText(DELETED_FROM_FAV));
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();
        basePageSteps.onMainPage().popup().button("Удалить все 10").click();

        basePageSteps.onFavoritesPage().notifier().should(hasText(DELETED_FROM_FAV));
        basePageSteps.onFavoritesPage().salesList().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем один неактивный оффер категории «Коммерческий» через попап массового удаления")
    public void shouldSeeDeleteOneNotActiveOfferCommercial() {
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().notifier().waitUntil(hasText(DELETED_FROM_FAV));

        mockRule.with("desktop/UserFavoritesTrucks2Delete").update();
        favoritesCount = basePageSteps.onFavoritesPage().salesList().size();
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().popup().button(DELETE_ONLY_THIS).click();

        basePageSteps.onFavoritesPage().notifier().should(hasText(DELETED_FROM_FAV));
        basePageSteps.onFavoritesPage().salesList().should(hasSize(favoritesCount - 1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем один неактивный оффер категории «Мото» через попап массового удаления")
    public void shouldSeeDeleteOneNotActiveOfferMoto() {
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().notifier().waitUntil(hasText(DELETED_FROM_FAV));

        mockRule.with("desktop/UserFavoritesMoto2Delete").update();
        favoritesCount = basePageSteps.onFavoritesPage().salesList().size();
        basePageSteps.onFavoritesPage().getSale(1).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().popup().button(DELETE_ONLY_THIS).click();

        basePageSteps.onFavoritesPage().notifier().should(hasText(DELETED_FROM_FAV));
        basePageSteps.onFavoritesPage().salesList().should(hasSize(favoritesCount - 1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаляем один неактивный оффер категории «Авто» через попап массового удаления")
    public void shouldSeeDeleteOneNotActiveOfferCar() {
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().notifier().waitUntil(hasText(DELETED_FROM_FAV));

        mockRule.with("desktop/UserFavoritesCars2Delete").update();
        favoritesCount = basePageSteps.onFavoritesPage().salesList().size();
        basePageSteps.onFavoritesPage().getSale(2).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().popup().button(DELETE_ONLY_THIS).click();

        basePageSteps.onFavoritesPage().notifier().should(hasText(DELETED_FROM_FAV));
        basePageSteps.onFavoritesPage().salesList().should(hasSize(favoritesCount - 1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрываем попап удаления неактивных офферов")
    public void shouldSeeCloseDeleteAllNotActiveOffersPopup() {
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().notifier().waitUntil(hasText(DELETED_FROM_FAV));
        favoritesCount = basePageSteps.onFavoritesPage().salesList().size();
        basePageSteps.onFavoritesPage().getSale(0).addToFavoritesIcon().click();
        basePageSteps.onFavoritesPage().popup().closeIcon().waitUntil(isDisplayed()).click();

        basePageSteps.onFavoritesPage().popup().should(not(isDisplayed()));
        basePageSteps.onFavoritesPage().salesList().should(hasSize(favoritesCount));
    }

}
