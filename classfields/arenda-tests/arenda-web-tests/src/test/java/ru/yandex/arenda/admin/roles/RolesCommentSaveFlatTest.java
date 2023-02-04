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
import ru.yandex.arenda.element.lk.admin.ManagerFlatItem;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.FLATS;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.pages.AdminUserPage.CONCIERGE_COMMENT_ID;
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
public class RolesCommentSaveFlatTest {

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
        id = retrofitApiSteps.createConfirmedFlat(uid);
        passportSteps.adminLogin();

        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(id)
                .queryParam(DEGRADATION_ROLE_PARAM, ADMIN_ROLE_VALUE).queryParam(ROLE_PARAM, role).open();
        testComment = getRandomString();
        lkSteps.onAdminFlatPage().textAreaId(CONCIERGE_COMMENT_ID).sendKeys(testComment);
        lkSteps.onAdminFlatPage().conciergeSaveCommentButton().click();
        lkSteps.onAdminFlatPage().successToast();
    }

    @Test
    @DisplayName("Видим сохранение комментария для квартиры")
    public void shouldSeeSavedComment() {
    }

    @Test
    @DisplayName("Видим комментарий на листинге для квартиры")
    public void shouldSeeSavedCommentOnListing() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        getCommentedItem().conciergeCommentLink().click();
        getCommentedItem().conciergeComment().should(hasText(testComment));
    }

    @Test
    @DisplayName("Меняем комментарий. Видим на листинге для квартиры")
    public void shouldSeeEditedCommentOnListing() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        getCommentedItem().conciergeCommentLink().click();
        getCommentedItem().conciergeComment().waitUntil(hasText(testComment));

        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(id)
                .queryParam(DEGRADATION_ROLE_PARAM, ADMIN_ROLE_VALUE).queryParam(ROLE_PARAM, role).open();
        secondTestComment = getRandomString();
        lkSteps.clearInputByBackSpace(() -> lkSteps.onAdminFlatPage().textAreaId(CONCIERGE_COMMENT_ID));
        lkSteps.onAdminFlatPage().textAreaId(CONCIERGE_COMMENT_ID).sendKeys(secondTestComment);
        lkSteps.onAdminFlatPage().conciergeSaveCommentButton().click();
        lkSteps.onAdminFlatPage().successToast();

        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        getCommentedItem().conciergeCommentLink().click();
        getCommentedItem().conciergeComment().waitUntil(hasText(secondTestComment));
    }

    private ManagerFlatItem getCommentedItem() {
        return lkSteps.onAdminListingPage().managerFlatsItem().filter(item -> item.link().getAttribute("href")
                .contains(id)).get(0);
    }
}
