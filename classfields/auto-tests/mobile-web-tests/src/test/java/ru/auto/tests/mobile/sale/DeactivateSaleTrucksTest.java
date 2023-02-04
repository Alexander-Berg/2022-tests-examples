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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - снятие с продажи")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DeactivateSaleTrucksTest {

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
        mockRule.newMock().with("desktop/OfferTrucksUsedUserOwner",
                "desktop/SessionAuthUser").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/UserOffersTrucksHide",
                "desktop/OfferTrucksUsedUserOwnerInactive").post();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи")
    public void shouldDeactivateSale() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().ownerControls().button("Снять с продажи"));
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().button("Снять с продажи").click();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Статус объявления изменен"));
        basePageSteps.onCardPage().soldPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().ownerControls().button("Снять с продажи").waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().ownerControls().button("Активировать").waitUntil(isDisplayed());
    }
}
