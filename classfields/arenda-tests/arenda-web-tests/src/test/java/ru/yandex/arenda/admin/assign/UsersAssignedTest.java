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

import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USERS;
import static ru.yandex.arenda.pages.AdminAssignedUserPage.ASSIGN_BUTTON;
import static ru.yandex.arenda.pages.AdminAssignedUserPage.USER_SUGGEST_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@Link("https://st.yandex-team.ru/VERTISTEST-1731")
@DisplayName("[Админка] Привязка и отвязка пользователей в квартире и договоре")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UsersAssignedTest {

    private String uid;
    private String uidForAssign;
    private String createdFlatId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private Account accountForAssign;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("Привязываем пользователя как жильца")
    public void shouldSeeAssignedUser() {
        uid = account.getId();
        uidForAssign = accountForAssign.getId();
        retrofitApiSteps.createUser(uid);
        String name = "Жилец_" + getRandomString();
        retrofitApiSteps.createUserWithName(uidForAssign, name, name, name);
        String userName = retrofitApiSteps.getNameFormatted(uidForAssign);
        createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        retrofitApiSteps.unassignUser(createdFlatId, retrofitApiSteps.getUserId(uid));
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(USERS).open();
        lkSteps.onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).sendKeys(name);
        lkSteps.onAdminAssignedUserPage().suggestElement(userName).click();
        lkSteps.onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).waitUntil(hasValue(userName));
        lkSteps.onAdminAssignedUserPage().assignRolSelector().click();
        lkSteps.onAdminAssignedUserPage().selectorOption("Жилец").click();
        lkSteps.onAdminAssignedUserPage().button(ASSIGN_BUTTON).click();
        lkSteps.onAdminAssignedUserPage().successToast();
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet().userLink().should(hasText(userName));
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet().assignedRole().should(hasText(("Жилец")));
    }
}
