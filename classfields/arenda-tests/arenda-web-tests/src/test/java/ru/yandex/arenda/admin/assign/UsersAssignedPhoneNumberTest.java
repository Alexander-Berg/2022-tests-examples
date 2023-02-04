package ru.yandex.arenda.admin.assign;

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
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USERS;
import static ru.yandex.arenda.matcher.FindPatternMatcher.findPattern;
import static ru.yandex.arenda.pages.AdminAssignedUserPage.USER_SUGGEST_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Link("https://st.yandex-team.ru/VERTISTEST-1731")
@DisplayName("[Админка] Привязка и отвязка пользователей в квартире и договоре")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UsersAssignedPhoneNumberTest {

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
    }

    @Parameterized.Parameter
    public String phone;

    @Parameterized.Parameters(name = "Поиск по «{0}»")
    public static Collection<String> parameters() {
        return asList("904333", "+7 (904) 333-33-33", "+7904 333-33-33", "+79043333333");
    }

    @Test
    @DisplayName("Саждест пользователей -> поиск по телефону")
    public void shouldSeeAssignedByPhone() {
        account.getId();
        retrofitApiSteps.createUser(account.getId());
        String createdFlatId = retrofitApiSteps.createConfirmedFlat(account.getId());
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(USERS).open();
        lkSteps.onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).sendKeys(phone);
        lkSteps.onAdminAssignedUserPage().suggestList().waitUntil(hasSize(greaterThan(0))).forEach(item ->
                item.should(hasText(findPattern("\\+7 \\(904\\) 333-\\d{2}-\\d{2}"))));
    }
}
