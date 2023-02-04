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
import ru.auto.tests.publicapi.model.VertisPassportChangePasswordParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NoAuth;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 31.08.18.
 */

@DisplayName("POST /user/password")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class PasswordTest {
    private static final String DEFAULT_CURRENT_PASSWORD = "autoru";

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
        api.userPassword().changePassword().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        api.userPassword().changePassword().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee401WithoutSession() {
        api.userPassword().changePassword().body(new VertisPassportChangePasswordParameters())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NoAuth()));
    }

    @Test
    public void shouldSee401WithWrongCurrentPassword() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        api.userPassword().changePassword()
                .body(new VertisPassportChangePasswordParameters().currentPassword(Utils.getRandomString()).newPassword(Utils.getRandomString()))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldChangePassword() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        api.userPassword().changePassword()
                .body(new VertisPassportChangePasswordParameters().currentPassword(DEFAULT_CURRENT_PASSWORD).newPassword(Utils.getRandomString()))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }
}
