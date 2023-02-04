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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Пользователи. Удаление группы")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class DeleteGroupTest {

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
                "cabinet/DealerUsersGroupDelete").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(USERS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Удаление группы. К группе нет прикрепленных пользователей")
    public void shouldDeleteGroup() {
        steps.onCabinetUsersPage().groupsList().should(hasSize(3));
        steps.onCabinetUsersPage().getGroup(2).button("Удалить группу").click();
        steps.onCabinetUsersPage().popup()
                .waitUntil(hasText("Удалить группу\nГруппа с пользовательскими доступами будет удалена\nХорошо\n" +
                        "Отменить"));
        steps.onCabinetUsersPage().popup().button("Хорошо").click();
        steps.onCabinetUsersPage().notifier().waitUntil(hasText("Группа успешно удалена"));
        steps.onCabinetUsersPage().groupsList().waitUntil(hasSize(2));
        steps.onCabinetUsersPage().group("Группа <bnkp").should(not(isDisplayed()));
    }
}
