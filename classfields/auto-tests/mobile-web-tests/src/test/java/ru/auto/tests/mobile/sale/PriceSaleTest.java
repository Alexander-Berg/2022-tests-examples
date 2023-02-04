package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - цена")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class PriceSaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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
                "desktop/OfferCarsUsedUser",
                "desktop/UserFavoritesCarsPost",
                "desktop/UserFavoritesCarsDelete").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Следить за изменением цены»")
    public void shouldClickWatchPriceButton() {
        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().button("Следить за изменением цены").click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("В избранном 1 предложение\n" +
                "Смотреть"));
        basePageSteps.onCardPage().cardActions().deleteFromFavoritesButton().waitUntil(isDisplayed());

        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().button("Отписаться").click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Удалено из избранного"));
        basePageSteps.onCardPage().cardActions().addToFavoritesButton().waitUntil(isDisplayed());
    }
}
