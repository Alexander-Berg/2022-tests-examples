package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - скидки")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class SaleDiscountsTest {

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
        mockRule.newMock().with("desktop/OfferCarsNewDealerDiscount").post();

        urlSteps.testing().path(CARS).path(NEW).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение цены cо скидкой")
    public void shouldSeeDiscountPrice() {
        basePageSteps.onCardPage().price().should(hasText("от 2 319 642 ₽\n2 480 900 ₽ без скидок"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа cо скидками")
    public void shouldSeeDiscountPopup() {
        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Цена\nот 2 319 642 ₽\n" +
                "Цена без скидки\n2 480 900 ₽\n · \n38 908 $\n · \n35 284 €\nСкидки\nМаксимальная\n161 258 ₽\n" +
                "Максимальная скидка, которую может предоставить дилер. Подробности узнавайте по телефону.\n" +
                "Позвонить"));
    }
}
