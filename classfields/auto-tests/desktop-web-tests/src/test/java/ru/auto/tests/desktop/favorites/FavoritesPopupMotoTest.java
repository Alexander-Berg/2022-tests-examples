package ru.auto.tests.desktop.favorites;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поп-ап избранного")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FavoritesPopupMotoTest {

    private static final String SALE_ID = "/4102039-a52db929/";
    private static final int FAVORITES_COUNT = 3;
    private static final int FAVORITE_ID = 1;

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
                "desktop/OfferCarsUsedUser",
                "desktop/UserFavoritesAll").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/1076842087-f1e84/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на карточку объявления из поп-апа избранного")
    public void shouldClickSale() {
        mockRule.with("desktop/OfferMotoUsedUser2").update();

        String title = basePageSteps.onCardPage().cardHeader().firstLine().getText();
        String sellerComment = basePageSteps.onCardPage().sellerComment().getText();
        basePageSteps.onCardPage().header().favoritesButton().click();
        basePageSteps.onCardPage().favoritesPopup().favoritesList().should(hasSize(FAVORITES_COUNT)).get(FAVORITE_ID)
                .link().click();
        basePageSteps.onCardPage().favoritesPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path("/honda/cbr_300/").path(SALE_ID)
                .shouldNotSeeDiff();
        basePageSteps.onCardPage().cardHeader().firstLine().should(not(hasText(title)));
        basePageSteps.onCardPage().sellerComment().should(not(hasText(sellerComment)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать телефон»")
    public void shouldClickShowPhoneButton() {
        mockRule.with("desktop/OfferMotoPhones").update();

        basePageSteps.onCardPage().header().favoritesButton().click();
        basePageSteps.onCardPage().favoritesPopup().getFavorite(FAVORITE_ID).button("Показать телефон").click();
        basePageSteps.onCardPage().contactsPopup().phones().waitUntil(hasText("+7 916 039-84-27\nc 10:00 до 23:00\n" +
                "+7 916 039-84-28\nc 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение статистики звонков")
    public void shouldSeeCallsStats() {
        mockRule.with("desktop/OfferMotoCallsStats").update();

        basePageSteps.onCardPage().header().favoritesButton().click();
        basePageSteps.onCardPage().favoritesPopup().getFavorite(FAVORITE_ID).callsStats().waitUntil(isDisplayed())
                .should(hasText("3 звонка за 24 часа"));
    }
}