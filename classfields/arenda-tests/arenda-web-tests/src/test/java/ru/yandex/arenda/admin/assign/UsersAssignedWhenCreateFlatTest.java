package ru.yandex.arenda.admin.assign;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.account.FlatsKeeper;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USER;
import static ru.yandex.arenda.constants.UriPath.USERS;
import static ru.yandex.arenda.steps.RetrofitApiSteps.PATH_TO_POST_FLAT_DRAFT;
import static ru.yandex.arenda.utils.UtilsWeb.getObjectFromJson;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Link("https://st.yandex-team.ru/VERTISTEST-1731")
@DisplayName("[Админка] Привязка и отвязка пользователей в квартире и договоре")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UsersAssignedWhenCreateFlatTest {

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

    @Inject
    private FlatsKeeper flatsKeeper;

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("В квартире созданной через лк Доступна вкладка Пользователи, есть привязанный собственник.")
    public void shouldSeeAssignedUser() {
        uid = account.getId();
        retrofitApiSteps.createUser(uid);
        createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(USERS).open();
        String personText = retrofitApiSteps.getNameFormatted(uid);
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet().userLink().should(hasText(personText));
    }

    @Test
    @DisplayName("Ссылка пользователя ведет на страницу пользователя")
    public void shouldSeeAssignedUserPage() {
        uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(USERS).open();
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet().userLink().click();
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).path("/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Есть привязанные пользователи, но без заполненных личных данных")
    public void shouldSeeAssignedNotFilledUser() {
        uid = account.getId();
        retrofitApiSteps.getUserByUid(uid);
        createdFlatId = retrofitApiSteps.postFlatDraft(uid, getObjectFromJson(JsonObject.class, PATH_TO_POST_FLAT_DRAFT));
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(USERS).open();
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet().userLink().should(hasText(containsString("id:")));
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet()
                .should(hasText(containsString("У пользователя не заполнены личные данные")));
    }
}
