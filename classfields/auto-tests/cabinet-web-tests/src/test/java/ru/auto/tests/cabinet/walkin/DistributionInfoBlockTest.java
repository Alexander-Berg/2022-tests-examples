package ru.auto.tests.cabinet.walkin;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALK_IN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Приезды в салон - блок информации о распределении по полу и возрасту")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class DistributionInfoBlockTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerWalkInStats",
                "cabinet/DealerTariff/CarsUsedOn").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALK_IN).open();
    }

    @Test
    @Category({Regression.class, Testing.class, Screenshot.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение блока информации о распределении по полу и возрасту")
    public void shouldSeeDistributionInfoBlock() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalkInPage().distributionInfoBlockExpanded());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalkInPage().distributionInfoBlockExpanded());
        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Сворачивание и разворачивание блока информации о распределении по полу и возрасту")
    public void shouldCollapseExpandDistributionInfoBlock() {
        steps.onCabinetWalkInPage().distributionInfoBlockExpanded().collapseButton().should(hasText("Скрыть")).click();
        steps.onCabinetWalkInPage().distributionInfoBlockExpanded().should(not(isDisplayed()));
        steps.onCabinetWalkInPage().distributionInfoBlockCollapsed().should(isDisplayed());

        steps.onCabinetWalkInPage().distributionInfoBlockCollapsed().collapseButton().should(hasText("Подробнее"))
                .click();
        steps.onCabinetWalkInPage().distributionInfoBlockExpanded().should(isDisplayed());
        steps.onCabinetWalkInPage().distributionInfoBlockCollapsed().should(not(isDisplayed()));
    }
}
