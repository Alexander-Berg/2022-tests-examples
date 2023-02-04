package ru.auto.tests.publicapi.phones;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.VertisPassportConfirmPhoneParameters;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


/**
 * Created by vicdev on 18.09.17.
 */

@DisplayName("POST /user/phones/confirm")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class ConfirmTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userPhones().confirmPhone().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        api.userPhones().confirmPhone().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee404WithWrongCode() {
        Account account = am.create();
        AutoApiErrorResponse response = api.userPhones().confirmPhone().reqSpec(defaultSpec()).body(new VertisPassportConfirmPhoneParameters()
                .code(Utils.getRandomString()).phone(account.getLogin()))
                .execute(validatedWith(shouldBeCode(HttpStatus.SC_NOT_FOUND))).as(AutoApiErrorResponse.class);
        assertThat(response).hasStatus(ERROR);
    }

    @Test
    @Ignore("Нет возможности получить код из телефона")
    public void shouldLogin() {
    }

    @Test
    @Ignore("Нет возможности получить код из телефона")
    public void shouldRegister() {

    }
}
