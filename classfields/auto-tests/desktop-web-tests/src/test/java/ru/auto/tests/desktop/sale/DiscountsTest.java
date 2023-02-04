package ru.auto.tests.desktop.sale;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление - скидки")
@Feature(AutoruFeatures.SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DiscountsTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedDealerDiscount").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение скидок")
    public void shouldSeeDiscounts() {
        basePageSteps.onCardPage().cardHeader().price().should(hasText("2 480 900 ₽\nот 1 910 900 ₽ со скидками"));
        basePageSteps.onCardPage().cardHeader().price().rubPrice().hover();
        basePageSteps.onCardPage().pricePopup().waitUntil(isDisplayed()).should(hasText("2 480 900 ₽\nот 1 910 900 ₽ со скидками\n" +
                "38 908 $\n · \n35 284 €\nСкидки\nВ кредит\nдо 250 000 ₽\nС каско\nдо 20 000 ₽\nВ трейд-ин\nдо 300 000 ₽\n" +
                "Максимальная\n570 000 ₽\nМаксимальная скидка, которую может предоставить дилер. Подробности узнавайте по телефону."));

        basePageSteps.onCardPage().discounts().should(hasText("В кредит\nдо 250 000 ₽\nВ трейд-ин\nдо 300 000 ₽\n" +
                "С каско\nдо 20 000 ₽\nМаксимальная\n570 000 ₽\nМаксимальная скидка, которую может предоставить дилер. " +
                "Подробности узнавайте по телефону."));
    }
}
