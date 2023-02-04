package ru.auto.tests.cabinet.calls;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@DisplayName("Кабинет дилера. Звонки. Настройки")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsSettingsTest {

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

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff/AllTariffs",
                "cabinet/CommonCustomerGet",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaigns",
                "cabinet/ApiAccessClient",
                "cabinet/Calltracking",
                "cabinet/CalltrackingAggregated",
                "cabinet/CalltrackingSettings").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).path(SETTINGS).open();

    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение страницы")
    public void shouldSeePage() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsSettingsPage().content());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsSettingsPage().content());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Включение/выключение трекинга звонков")
    public void shouldTurnCalltrackingOnAndOff() {
        mockRule.with("cabinet/CalltrackingSettingsCalltrackingEnabledTruePut",
                "cabinet/CalltrackingSettingsCalltrackingEnabledFalsePut").update();

        basePageSteps.onCallsSettingsPage().inactiveToggle("Трекинг звонков").click();
        basePageSteps.onCallsSettingsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Настройки успешно обновлены"));
        basePageSteps.onCallsSettingsPage().notifier().waitUntil(not(isDisplayed()));

        basePageSteps.onCallsSettingsPage().activeToggle("Трекинг звонков").click();
        basePageSteps.onCallsSettingsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Настройки успешно обновлены"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Включение/выключение статистики по объявлениям")
    public void shouldTurnSaleStatsOnAndOff() {
        mockRule.with("cabinet/CalltrackingSettingsStatsEnabledTruePut",
                "cabinet/CalltrackingSettingsStatsEnabledFalsePut").update();

        basePageSteps.onCallsSettingsPage().activeToggle("Статистика по объявлениям").click();
        basePageSteps.onCallsSettingsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Настройки успешно обновлены"));
        basePageSteps.onCallsSettingsPage().notifier().waitUntil(not(isDisplayed()));

        basePageSteps.onCallsSettingsPage().inactiveToggle("Статистика по объявлениям").click();
        basePageSteps.onCallsSettingsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Настройки успешно обновлены"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Изменение параметров в блоке «Уникальный звонок»")
    public void shouldChangeUniqueParams() {
        mockRule.with("cabinet/CalltrackingSettingsUniqueCallPeriodPut").update();

        basePageSteps.onCallsSettingsPage().clearInput("Количество дней");
        basePageSteps.onCallsSettingsPage().input("Количество дней", "7");
        basePageSteps.onCallsSettingsPage().title().click();
        basePageSteps.onCallsSettingsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Настройки успешно обновлены"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Изменение параметров в блоке «Целевой звонок»")
    public void shouldChangeTargetParams() {
        mockRule.with("cabinet/CalltrackingSettingsTargetCallDurationPut").update();

        basePageSteps.onCallsSettingsPage().clearInput("Длительность входящего вызова");
        basePageSteps.onCallsSettingsPage().input("Длительность входящего вызова", "120");
        basePageSteps.onCallsSettingsPage().title().click();
        basePageSteps.onCallsSettingsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Настройки успешно обновлены"));
    }
}
