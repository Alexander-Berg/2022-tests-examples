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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Пользователи. Удаление пользователя")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class DeleteUserTest {

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
                "cabinet/DealerUserDelete").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(USERS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Удалить пользователя")
    public void shouldClickDeleteUser() {
        steps.onCabinetUsersPage().getGroup(1).getUser(0).button("Удалить из кабинета").click();
        steps.onCabinetUsersPage().popup().waitUntil(hasText("Удалить пользователя\n" +
                "Пользовать будет удалён из кабинета\nХорошо\nОтменить"));
        steps.onCabinetUsersPage().popup().button("Хорошо").click();
        steps.onCabinetUsersPage().notifier().waitUntil(hasText("Пользователь успешно удалён"));
        steps.onCabinetUsersPage().getGroup(1).user("Мужиков Евгений").should(not(isDisplayed()));
    }
}
