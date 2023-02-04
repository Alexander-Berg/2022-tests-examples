package ru.auto.tests.desktop.vas;

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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Объявление - автоподнятие")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasAutoFreshTest {

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/OfferCarsUsedUserOwner",
                "desktop/SessionAuthUser",
                "desktop/UserWithTiedCard",
                "desktop/BillingAutoruPaymentInitAutoFresh",
                "desktop/BillingAutoruPayment",
                "desktop/BillingSchedules",
                "desktop/BillingSchedulesCarsBoost").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа")
    public void shouldSeeAutoFreshPopup() {
        mockRule.with("desktop/BillingAutoruPaymentProcessAutoFresh").update();

        basePageSteps.onCardPage().cardVas().tab("Поднятие в поиске").click();
        basePageSteps.onCardPage().cardVas().buyButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().checkboxContains("Включить автоподнятие").click();
        basePageSteps.onCardPage().billingPopup().tiedCardPayButton().click();
        basePageSteps.onCardPage().billingPopup().waitUntil(hasText(matchesPattern("Поднятие в поиске\n97 ₽\n" +
                "Платёж совершён успешно\nХотите, чтобы ваше объявление было наверху каждый день\\?\n" +
                "Включить автоподнятие за 97 ₽ в день\nв \\d+:\\d+\nСовершая платеж, " +
                "вы соглашаетесь с условиями Оферты")));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Опция «Поднятие в поиске» подключена"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Включение автоподнятия")
    public void shouldTurnOnAutoFreshInPopup() {
        mockRule.with("desktop/BillingAutoruPaymentProcessAutoFresh").update();

        basePageSteps.onCardPage().cardVas().tab("Поднятие в поиске").click();
        basePageSteps.onCardPage().cardVas().buyButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().checkboxContains("Включить автоподнятие").click();
        basePageSteps.onCardPage().billingPopup().tiedCardPayButton().click();
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Опция «Поднятие в поиске» подключена"));
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().autoFreshButton().waitUntil(isDisplayed()).click();
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Автоподнятие включено"));
        basePageSteps.onCardPage().billingPopup().waitUntil(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Включение автоподнятия по дефолту")
    public void shouldTurnOnAutoFreshByDefault() {
        mockRule.with("desktop/BillingAutoruPaymentProcessAutoFreshDefault").update();

        basePageSteps.onCardPage().cardVas().tab("Поднятие в поиске").click();
        basePageSteps.onCardPage().cardVas().buyButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().tiedCardPayButton().click();
        basePageSteps.switchToDefaultFrame();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Опция «Поднятие в поиске» подключена"));
        basePageSteps.onCardPage().billingPopup().waitUntil(not(isDisplayed()));
    }
}