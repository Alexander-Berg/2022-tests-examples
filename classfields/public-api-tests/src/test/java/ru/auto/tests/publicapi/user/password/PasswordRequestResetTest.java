package ru.auto.tests.publicapi.user.password;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.VertisPassportRequestPasswordResetParameters;
import ru.auto.tests.publicapi.model.VertisPassportUserIdentity;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.IDENTITY_IS_MISSING;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 31.08.18.
 */

@DisplayName("POST /user/password/request-reset")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PasswordRequestResetTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userPassword().requestPasswordReset().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        api.userPassword().requestPasswordReset().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithoutSessionIdAndIdentity() {
        AutoApiErrorResponse response = api.userPassword().requestPasswordReset().body(new VertisPassportRequestPasswordResetParameters())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);

        assertThat(response).hasError(IDENTITY_IS_MISSING).hasStatus(ERROR).hasDetailedError("IDENTITY_IS_MISSING");
    }

    @Test
    public void shouldRequestPasswordChangeWithoutSessionIdWithIdentity() {
        Account account = am.create();

        api.userPassword().requestPasswordReset()
                .body(new VertisPassportRequestPasswordResetParameters()
                        .phone(account.getPhone().toString())
                        .identity(new VertisPassportUserIdentity().phone(account.getPhone().toString())))
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldRequestPasswordChangeWithSessionIdWithoutIdentity() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        api.userPassword().requestPasswordReset().body(new VertisPassportRequestPasswordResetParameters().phone(account.getPhone().toString()))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }
}
