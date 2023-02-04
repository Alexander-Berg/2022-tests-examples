package ru.auto.tests.publicapi.user.password;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.VertisPassportResetPasswordParameters;
import ru.auto.tests.publicapi.model.VertisPassportUserIdentity;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 31.08.18.
 */

@DisplayName("POST /user/password/reset")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PasswordResetTest {
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
        api.userPassword().resetPassword().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        api.userPassword().resetPassword().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee404WithIncorrectCode() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String code = Utils.getRandomString();

        api.userPassword().resetPassword()
                .body(new VertisPassportResetPasswordParameters()
                        .code(code).newPassword(Utils.getRandomString())
                        .identity(new VertisPassportUserIdentity().phone(account.getPhone().toString())))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldResetPassword() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.createPasswordChangeRequest(sessionId, account.getPhone().toString());
        String code = adaptor.getSmsCode(account.getId(), 0);

        api.userPassword().resetPassword()
                .body(new VertisPassportResetPasswordParameters()
                        .code(code).newPassword(Utils.getRandomString())
                        .identity(new VertisPassportUserIdentity().phone(account.getPhone().toString())))
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }
}