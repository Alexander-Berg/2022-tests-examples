package ru.yandex.arenda.admin.roles;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.element.lk.admin.ManagerUserItem;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.OTHER;
import static ru.yandex.arenda.constants.UriPath.USER;
import static ru.yandex.arenda.constants.UriPath.USERS;
import static ru.yandex.arenda.pages.AdminUserPage.CONCIERGE_COMMENT_ID;
import static ru.yandex.arenda.pages.AdminUserPage.SAVE_BUTTON;
import static ru.yandex.arenda.steps.UrlSteps.ADMIN_ROLE_VALUE;
import static ru.yandex.arenda.steps.UrlSteps.CONCIERGE_MANAGER_EXTENDED_ROLE_VALUE;
import static ru.yandex.arenda.steps.UrlSteps.CONCIERGE_MANAGER_ROLE_VALUE;
import static ru.yandex.arenda.steps.UrlSteps.DEGRADATION_ROLE_PARAM;
import static ru.yandex.arenda.steps.UrlSteps.ROLE_PARAM;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Link("https://st.yandex-team.ru/VERTISTEST-2058")
@DisplayName("Админка. Комментарии. Сохранение комментариев")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RolesCommentSaveUserTest {

    private String id;
    private String testComment;
    private String secondTestComment;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String user;

    @Parameterized.Parameter(1)
    public String role;

    @Parameterized.Parameters(name = "Страница «{0} ({1})»")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"Роль 1", CONCIERGE_MANAGER_ROLE_VALUE},
                {"Роль 2", CONCIERGE_MANAGER_EXTENDED_ROLE_VALUE}
        });
    }

    @Before
    public void before() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        id = retrofitApiSteps.getUserId(uid);
        passportSteps.adminLogin();

        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(id).path(OTHER)
                .queryParam(DEGRADATION_ROLE_PARAM, ADMIN_ROLE_VALUE).queryParam(ROLE_PARAM, role).open();
        testComment = getRandomString();
        lkSteps.onAdminUserPage().textAreaId(CONCIERGE_COMMENT_ID).sendKeys(testComment);
        lkSteps.onAdminUserPage().button(SAVE_BUTTON).click();
        lkSteps.onAdminUserPage().successToast();
    }

    @Test
    @DisplayName("Видим сохранение комментария для пользователя")
    public void shouldSeeSavedComment() {
    }

    @Test
    @DisplayName("Видим комментарий на листинге для пользователя")
    public void shouldSeeSavedCommentOnListing() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USERS).open();
        getCommentedItem().conciergeCommentLink().click();
        getCommentedItem().conciergeComment().should(hasText(testComment));
    }

    @Test
    @DisplayName("Меняем комментарий. Видим на листинге для пользователя")
    public void shouldSeeEditedCommentOnListing() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USERS).open();
        getCommentedItem().conciergeCommentLink().click();
        getCommentedItem().conciergeComment().waitUntil(hasText(testComment));

        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(id).path(OTHER)
                .queryParam(DEGRADATION_ROLE_PARAM, ADMIN_ROLE_VALUE).queryParam(ROLE_PARAM, role).open();
        secondTestComment = getRandomString();
        lkSteps.clearInputByBackSpace(() -> lkSteps.onAdminUserPage().textAreaId(CONCIERGE_COMMENT_ID));
        lkSteps.onAdminUserPage().textAreaId(CONCIERGE_COMMENT_ID).sendKeys(secondTestComment);
        lkSteps.onAdminUserPage().button(SAVE_BUTTON).click();
        lkSteps.onAdminUserPage().successToast();

        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USERS).open();
        getCommentedItem().conciergeCommentLink().click();
        getCommentedItem().conciergeComment().waitUntil(hasText(secondTestComment));
    }

    private ManagerUserItem getCommentedItem() {
        return lkSteps.onAdminListingPage().managerUsersItem().filter(item -> item.link().getAttribute("href")
                .contains(id)).get(0);
    }
}
