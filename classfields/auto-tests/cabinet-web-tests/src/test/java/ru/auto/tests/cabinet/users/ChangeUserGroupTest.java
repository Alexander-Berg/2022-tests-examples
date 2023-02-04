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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Пользователи. Изменение группы пользователя")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ChangeUserGroupTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/DealerAccessResources",
                "cabinet/DealerUsers",
                "cabinet/DealerUsersGroups",
                "cabinet/DealerUserPut").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(USERS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Сменить группу пользователя")
    public void shouldChangeUserGroup() {
        steps.onCabinetUsersPage().getGroup(1).getUser(0).hover();
        steps.onCabinetUsersPage().getGroup(1).getUser(0).button("Сменить группу").click();
        steps.onCabinetUsersPage().editPopup().selectItem("1234567890", "Администратор");
        steps.onCabinetUsersPage().editPopup().button("Сохранить").click();
        steps.onCabinetUsersPage().notifier().waitUntil(hasText("Пользователь перенесен в группу «Администратор»"));
        steps.onCabinetUsersPage().getGroup(0).getUser(1).userName().waitUntil(hasText("Мужиков Евгений"));
        steps.onCabinetUsersPage().getGroup(1).usersList().should(hasSize(0));
    }
}
