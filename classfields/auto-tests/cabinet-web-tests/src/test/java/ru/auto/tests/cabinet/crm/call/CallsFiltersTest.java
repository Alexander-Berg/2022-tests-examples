package ru.auto.tests.cabinet.crm.call;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MANAGER;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Менеджер. Звонки. Фильтры")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsFiltersTest {

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
        mockRule.newMock().with("cabinet/Session/Manager",
                "cabinet/ApiAccessClientManager",
                "cabinet/CommonCustomerGetManager",
                "cabinet/CalltrackingExport",
                "cabinet/CalltrackingReset",
                "cabinet/Calltracking",
                "cabinet/CalltrackingAggregatedReset",
                "cabinet/CalltrackingAggregated").post();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение фильтров")
    public void shouldSeeFilters() {
        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).addParam(CLIENT_ID, "16453").open();
        screenshotSteps.setWindowSizeForScreenshot();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsPage().filters());

        urlSteps.setProduction().open();
        screenshotSteps.setWindowSizeForScreenshot();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsPage().filters());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Экспорт»")
    public void shouldClickExportButton() {
        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).addParam(CLIENT_ID, "16453").open();
        basePageSteps.onCallsPage().filters().button("Экспорт").click();
        basePageSteps.onCallsPage().notifier().waitUntil(isDisplayed()).should(hasText("Файл с экспортом звонков успешно " +
                "загружен"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сброс фильтров")
    public void shouldResetFilters() {
        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).addParam(CLIENT_ID, "16453")
                .replaceQuery("call_result=ANSWERED_GROUP&call_target=TARGET_GROUP" +
                        "&callback=CALLBACK_GROUP&category=CARS&client_phone=%2B79111111111&from=2020-05-01" +
                        "&salon_phone=%2B79111111111&section=USED&tag=111&unique=ONLY_UNIQUE").open();
        basePageSteps.onCallsPage().filters().resetButton().click();
        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).shouldNotSeeDiff();
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(20));
    }
}