package ru.auto.tests.publicapi.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.VertisPassportLoginOrRegisterParameters;
import ru.auto.tests.publicapi.model.AutoApiLoginOrRegisterResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


/**
 * Created by vicdev on 14.09.17.
 */

@DisplayName("POST /auth/login-or-register")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class LoginOrRegisterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    private static final int CODE_LENGTH = 4;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.auth().loginOrRegister().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        api.auth().loginOrRegister().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldLogin() {
        AutoApiLoginOrRegisterResponse response = api.auth().loginOrRegister().reqSpec(defaultSpec())
                .body(new VertisPassportLoginOrRegisterParameters()
                        .phone(am.create().getLogin())
                        .suppressNotifications(false))
                .executeAs(validatedWith(shouldBeSuccess()));
        assertThat(response).hasCodeLength(CODE_LENGTH);
    }

    @Test
    public void shouldRegister() {
        AutoApiLoginOrRegisterResponse response = api.auth().loginOrRegister().reqSpec(defaultSpec())
                .body(new VertisPassportLoginOrRegisterParameters()
                        .phone(Utils.getRandomPhone())
                        .suppressNotifications(false))
                .executeAs(validatedWith(shouldBeSuccess()));
        assertThat(response).hasCodeLength(CODE_LENGTH);
    }
}

