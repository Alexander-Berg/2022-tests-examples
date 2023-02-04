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
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Страница «Пользователи»")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class GroupsTest {

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
    @DisplayName("Отображение страницы «Пользователи»")
    public void shouldSeeUsersPage() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetUsersPage().usersPage());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetUsersPage().usersPage());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Поп-ап добавления новой группы")
    public void shouldSeeAddNewGroupPopup() {
        steps.onCabinetUsersPage().button("Добавить группу").click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetUsersPage().accessBlock());

        urlSteps.setProduction().open();
        steps.onCabinetUsersPage().button("Добавить группу").click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetUsersPage().accessBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Добавление новой группы")
    public void shouldAddNewGroup() {
        mockRule.with("cabinet/DealerUsersGroupPost").update();

        steps.onCabinetUsersPage().button("Добавить группу").click();
        steps.onCabinetUsersPage().accessBlock().input("Название группы", "Oasis");
        steps.onCabinetUsersPage().accessBlock().scopeBlock("Trade-In")
                .selectItem("Просмотр и изменение", "Только просмотр");
        steps.onCabinetUsersPage().accessBlock().scopeBlock("Звонки").checkbox().click();
        steps.onCabinetUsersPage().accessBlock().button("Создать группу").click();
        steps.onCabinetUsersPage().notifier().waitUntil(hasText("Группа успешно создана"));
        steps.onCabinetUsersPage().getGroup(3).title().waitUntil(hasText("Oasis\nУдалить группу"));
    }
}
