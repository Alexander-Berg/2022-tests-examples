package ru.auto.tests.cabinet.settings;

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
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.ACTIVATE_OFFER_AFTER_IDLE_TIME_SECTION;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.AUTOMATIC_PHOTO_ORDERING_SECTION;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 3.09.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Настройки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class SettingsPageScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet", "cabinet/DealerTariff",
                "cabinet/ApiSubscriptionsClient",
                "cabinet/DesktopSalonSilentPropertyUpdate").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SETTINGS).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Автоматический порядок фотографий»")
    public void shouldSeeBlockAutoPhotoOrder() {
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetSettingsPage().section(AUTOMATIC_PHOTO_ORDERING_SECTION).waitUntil
                        (isDisplayed()));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetSettingsPage().section(AUTOMATIC_PHOTO_ORDERING_SECTION).waitUntil
                        (isDisplayed()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Активировать объявления после простоя»")
    public void shouldSeeBlockActivateOffersAfterIdleTime() {
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetSettingsPage().section(ACTIVATE_OFFER_AFTER_IDLE_TIME_SECTION).waitUntil(isDisplayed()));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetSettingsPage().section(ACTIVATE_OFFER_AFTER_IDLE_TIME_SECTION).waitUntil(isDisplayed()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
