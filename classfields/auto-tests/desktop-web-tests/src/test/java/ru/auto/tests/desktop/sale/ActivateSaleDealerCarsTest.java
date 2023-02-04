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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - активация")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ActivateSaleDealerCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private final static String STATUS_INACTIVE = "Не опубликовано";
    private final static String STATUS_ACTIVE = "Опубликовано";

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
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "desktop/OfferCarsUsedDealerOwnerInactive").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().cardOwnerPanel().status().should(hasText(STATUS_INACTIVE));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Активация")
    public void shouldActivateSale() {
        mockRule.overwriteStub(1, "desktop/OfferCarsUsedDealerOwner");
        mockRule.with("desktop/UserOffersCarsActivate").update();

        basePageSteps.onCardPage().cardOwnerPanel().button("Опубликовать").click();
        basePageSteps.onCardPage().cardOwnerPanel().status().waitUntil(hasText(STATUS_ACTIVE));
        basePageSteps.onCardPage().cardOwnerPanel().button("Снять с продажи").waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Активация - недостаточно средств")
    public void shouldNotActivateSale() {
        mockRule.with("desktop/UserOffersCarsActivatePaymentNeeded").update();

        basePageSteps.onCardPage().cardOwnerPanel().button("Опубликовать").click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Недостаточно средств для оплаты. Пополните кошелёк."));
    }
}