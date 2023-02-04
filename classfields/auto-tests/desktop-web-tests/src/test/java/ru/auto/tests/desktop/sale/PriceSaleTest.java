package ru.auto.tests.desktop.sale;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
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
@GuiceModules(DesktopTestsModule.class)
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
                "desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа с ценой")
    public void shouldSeePricePopup() {
        basePageSteps.onCardPage().cardHeader().price().hover();
        basePageSteps.onCardPage().pricePopup().waitUntil(isDisplayed()).should(hasText("700 000 ₽\n10 418 $\n · \n" +
                "9 083 €\nот 20 734 ₽ / мес.\n20 августа 2018\nНачальная цена\n850 000 ₽\n28 августа 2018\n" +
                "- 20 000 ₽\n830 000 ₽\n13 сентября 2018\n- 30 000 ₽\n800 000 ₽\n4 октября 2018\n- 50 000 ₽\n" +
                "750 000 ₽\n17 октября 2018\n- 70 000 ₽\n680 000 ₽\n15 ноября 2018\n+ 20 000 ₽\n700 000 ₽\n" +
                "О скидках и акциях узнавайте по телефону\nСледить за изменением цены"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Следить за изменением цены»")
    public void shouldClickWatchPriceButton() {
        mockRule.with("desktop/UserFavoritesCarsPost",
                "desktop/UserFavoritesCarsDelete").update();

        basePageSteps.setWideWindowSize();
        basePageSteps.onCardPage().cardHeader().price().hover();
        basePageSteps.onCardPage().pricePopup().button("Следить за изменением цены").click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("В избранном 1 предложение" +
                "Перейти в избранное"));
        basePageSteps.onCardPage().header().favoritesButton().waitUntil(hasText("Избранное • 1"));

        basePageSteps.onCardPage().cardHeader().price().hover();
        basePageSteps.onCardPage().pricePopup().button("Отписаться").click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("Удалено из избранного"));
        basePageSteps.onCardPage().cardHeader().toolBar().favoriteButton().waitUntil(isDisplayed());
        basePageSteps.onCardPage().header().favoritesButton().waitUntil(hasText("Избранное"));
    }
}
