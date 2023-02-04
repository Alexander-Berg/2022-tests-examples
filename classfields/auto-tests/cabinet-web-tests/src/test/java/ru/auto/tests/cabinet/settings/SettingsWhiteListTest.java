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
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WHITELIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Настройки. Номера для выкупа")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class SettingsWhiteListTest {

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
                "cabinet/CommonCustomerGet",
                "cabinet/DealerPhonesWhitelistGet",
                "cabinet/DealerPhonesWhitelistAvailable").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SETTINGS).path(WHITELIST).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(TIMONDL)
    @DisplayName("Должны увидеть страничку с номерами и кнопками")
    public void shouldSeePageContent() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onSettingsWhiteListPage().pageContent());

        urlSteps.setProduction().open();

        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onSettingsWhiteListPage().pageContent());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должны увидеть попап добавления телефонов")
    public void shouldSeeAddPhonesPopup() {
        steps.onSettingsWhiteListPage().button("Добавить номера").click();
        steps.onSettingsWhiteListPage().popup().should(hasText("Добавление номеров\nВы можете скопировать список " +
                "номеров и вставить в поле ниже - они будут автоматически распознаны и добавлены в таблицу.\n" +
                "Возможно добавить не более 199 номеров.\nВведите номер телефона или несколько в свободном формате\n" +
                "Добавить"));
    }
}
