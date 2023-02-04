package ru.yandex.arenda.admin.assign;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USERS;
import static ru.yandex.arenda.pages.AdminAssignedUserPage.UNASSIGN_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1731")
@DisplayName("[Админка] Привязка и отвязка пользователей в квартире и договоре")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UsersAssignedDeleteAsssignTest {

    private String uid;
    private String createdFlatId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private Account account;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        passportSteps.adminLogin();
        uid = account.getId();
        retrofitApiSteps.createUser(uid);
        createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(USERS).open();
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet().deleteButton().click();
    }

    @Test
    @DisplayName("Отвязываем пользователя")
    public void shouldUnassignUser() {
        lkSteps.onAdminAssignedUserPage().unassignModal().button(UNASSIGN_BUTTON).click();
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet().should(not(isDisplayed()));
    }

    @Test
    @DisplayName("Отменяем отвязку пользователя")
    public void shouldCancelUnssignUser() {
        lkSteps.onAdminAssignedUserPage().unassignModal().button("Отмена").click();
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet().should(isDisplayed());
    }
}
