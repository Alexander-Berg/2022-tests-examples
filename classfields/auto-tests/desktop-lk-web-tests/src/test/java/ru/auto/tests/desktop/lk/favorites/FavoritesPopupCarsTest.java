package ru.auto.tests.desktop.lk.favorites;

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
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поп-ап избранного")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FavoritesPopupCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final int FAVORITES_COUNT = 3;
    private static final int FAVORITE_ID = 0;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody()),

                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop/UserOffersCarsEmpty"),
                stub("desktop/UserFavoritesAll")).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на карточку объявления из поп-апа избранного")
    public void shouldClickSale() {
        basePageSteps.onLkSalesPage().header().favoritesButton().click();
        basePageSteps.onLkSalesPage().favoritesPopup().favoritesList().should(hasSize(FAVORITES_COUNT)).get(FAVORITE_ID)
                .link().click();
        basePageSteps.onLkSalesPage().favoritesPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/").path(SALE_ID)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать телефон»")
    public void shouldClickShowPhoneButton() {
        mockRule.setStubs(stub("desktop/OfferCarsPhones")).update();

        basePageSteps.onLkSalesPage().header().favoritesButton().click();
        basePageSteps.onLkSalesPage().favoritesPopup().getFavorite(FAVORITE_ID).button("Показать телефон").click();
        basePageSteps.onLkSalesPage().contactsPopup().phones().waitUntil(hasText("+7 916 039-84-27\nc 10:00 до 23:00\n" +
                "+7 916 039-84-28\nc 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение статистики звонков")
    public void shouldSeeCallsStats() {
        mockRule.setStubs(stub("desktop/OfferCarsCallsStats")).update();

        basePageSteps.onLkSalesPage().header().favoritesButton().click();
        basePageSteps.onLkSalesPage().favoritesPopup().getFavorite(FAVORITE_ID).callsStats()
                .should(hasText("10 звонков за 24 часа"));
    }
}
