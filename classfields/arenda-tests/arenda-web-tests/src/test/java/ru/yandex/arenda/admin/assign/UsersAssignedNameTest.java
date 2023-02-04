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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USERS;
import static ru.yandex.arenda.pages.AdminAssignedUserPage.USER_SUGGEST_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@Link("https://st.yandex-team.ru/VERTISTEST-1731")
@DisplayName("[Админка] Привязка и отвязка пользователей в квартире и договоре")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UsersAssignedNameTest {

    private String uid;
    private String createdFlatId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

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
        uid = account.getId();
        retrofitApiSteps.createUser(uid);
        createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(USERS).open();
    }

    @Test
    @DisplayName("Саждест пользователей -> Проверяем пустой саджест привязки пользователей")
    public void shouldSeeEmptySuggest() {
        lkSteps.onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).sendKeys("ПОЛНАЯЧУШЬ");
        lkSteps.onAdminAssignedUserPage().suggestList().waitUntil(hasSize(greaterThan(0))).get(0)
                .should(hasText("Пользователи не найдены"));
    }

    @Test
    @DisplayName("Саждест пользователей -> ввести ФИО -> видим несколько вариантов")
    public void shouldSeeFewSuggest() {
        String textMatch = "Семенов";
        lkSteps.onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).sendKeys(textMatch);
        lkSteps.onAdminAssignedUserPage().suggestList().waitUntil(hasSize(greaterThan(1)))
                .forEach(element -> element.should(hasText(containsString(textMatch))));
    }

    @Test
    @DisplayName("Саждест пользователей -> ввести ФИО -> выбираем конкретное")
    public void shouldSeeExactSuggest() {
        String textMatch = "Семенов Валентин";
        lkSteps.onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).sendKeys(textMatch);
        lkSteps.onAdminAssignedUserPage().suggestElement(textMatch).click();
        lkSteps.onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).should(hasValue(textMatch));
    }
}
