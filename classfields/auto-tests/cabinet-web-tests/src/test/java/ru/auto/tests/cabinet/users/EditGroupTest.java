package ru.auto.tests.cabinet.users;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Пользователи. Редактирование группы")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class EditGroupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/DealerAccessResources",
                "cabinet/DealerUsers",
                "cabinet/DealerUsersGroups").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(USERS).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Открытие поп-апа редактирования группы")
    public void shouldOpenPopup() {
        steps.onCabinetUsersPage().getGroup(1).link("Редактировать").click();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetUsersPage().accessBlock());

        urlSteps.setProduction().open();
        steps.onCabinetUsersPage().getGroup(1).link("Редактировать").click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetUsersPage().accessBlock());
        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Редактировать группу. Изменение параметров и сохранение")
    public void shouldChangeParametersGroup() {
        mockRule.with("cabinet/DealerUsersGroupPut").update();

        steps.onCabinetUsersPage().getGroup(1).link("Редактировать").click();
        steps.onCabinetUsersPage().accessBlock().scopeBlock("Кошелёк")
                .selectItem("Просмотр и изменение", "Только просмотр");
        steps.onCabinetUsersPage().accessBlock().scopeBlock("Объявления").checkbox().click();
        steps.onCabinetUsersPage().accessBlock().button("Сохранить изменения").click();
        steps.onCabinetUsersPage().notifier().waitUntil(hasText("Группа успешно отредактирована"));
    }
}
