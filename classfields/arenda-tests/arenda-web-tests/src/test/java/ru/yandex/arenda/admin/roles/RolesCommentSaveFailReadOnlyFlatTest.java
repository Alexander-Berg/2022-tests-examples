package ru.yandex.arenda.admin.roles;

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
import static ru.yandex.arenda.pages.AdminUserPage.CONCIERGE_COMMENT_ID;
import static ru.yandex.arenda.steps.UrlSteps.ADMIN_READ_ONLY_VALUE;
import static ru.yandex.arenda.steps.UrlSteps.ADMIN_ROLE_VALUE;
import static ru.yandex.arenda.steps.UrlSteps.DEGRADATION_ROLE_PARAM;
import static ru.yandex.arenda.steps.UrlSteps.ROLE_PARAM;

@Link("https://st.yandex-team.ru/VERTISTEST-2058")
@DisplayName("Админка. Комментарии. Сохранение комментариев")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class RolesCommentSaveFailReadOnlyFlatTest {

    private String id;

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

    @Before
    public void before() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        id = retrofitApiSteps.createConfirmedFlat(uid);
        passportSteps.adminLogin();


    }

    @Test
    @DisplayName("Пытаемся сохранить комментарий для квартиры под ролью для чтения")
    public void shouldSeeSavedFailComment() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(id)
                .queryParam(DEGRADATION_ROLE_PARAM, ADMIN_ROLE_VALUE).queryParam(ROLE_PARAM, ADMIN_READ_ONLY_VALUE)
                .open();
        final String testComment = getRandomString();
        lkSteps.onAdminFlatPage().textAreaId(CONCIERGE_COMMENT_ID).sendKeys(testComment);
        lkSteps.onAdminFlatPage().conciergeSaveCommentButton().click();
        lkSteps.onAdminFlatPage().errorToast();
    }
}
