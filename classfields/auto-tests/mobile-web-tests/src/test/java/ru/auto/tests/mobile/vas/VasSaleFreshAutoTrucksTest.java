package ru.auto.tests.mobile.vas;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Автоподнятие на карточке")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VasSaleFreshAutoTrucksTest {

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
                "desktop/UserWithTiedCard",
                "desktop/OfferTrucksUsedUserOwnerWithServices",
                "mobile/BillingSchedulesTrucksBoostPut",
                "mobile/BillingSchedulesTrucksBoostDelete").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Включение/выключение автоподнятия в поп-апе")
    public void shouldOnOffAutoFreshInPopup() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vas()
                .service("Поднять в поиске за 97\u00a0₽").button("Автоподнятие за 87\u00a0₽ в день"));
        basePageSteps.onCardPage().popup().button("Включить за 87\u00a0₽ в день").click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Автоподнятие активировано"));
        basePageSteps.onCardPage().popup().title().waitUntil(hasText("Объявление ежедневно поднимается наверх"));
        basePageSteps.onCardPage().vas().service("Автоподнятие за 87\u00a0₽ в день").autoFreshTogglerActive()
                .waitUntil(isDisplayed());

        basePageSteps.onCardPage().popup().button("Выключить автоподнятие").click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Автоподнятие отключено"));
        basePageSteps.onCardPage().popup().title().waitUntil(hasText("Включите ежедневное поднятие объявления"));
        basePageSteps.onCardPage().vas().service("Автоподнятие за 87\u00a0₽ в день").autoFreshTogglerInactive()
                .waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Включение/выключение автоподнятия по тумблеру")
    public void shouldOnOffAutoFresh() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vas().service("Автоподнятие за 87\u00a0₽ в день")
                .autoFreshTogglerInactive());
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Автоподнятие активировано"));

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/UserWithTiedCard",
                "mobile/BillingSchedulesTrucksBoostDelete",
                "desktop/OfferTrucksUsedUserOwnerWithServices").post();

        basePageSteps.onCardPage().vas().service("Автоподнятие за 87\u00a0₽ в день").autoFreshTogglerActive()
                .waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Автоподнятие отключено"));
        basePageSteps.onCardPage().vas().service("Автоподнятие за 87\u00a0₽ в день").autoFreshTogglerInactive()
                .waitUntil(isDisplayed());
    }
}
