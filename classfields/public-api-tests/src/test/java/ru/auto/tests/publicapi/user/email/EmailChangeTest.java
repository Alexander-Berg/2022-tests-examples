package ru.auto.tests.publicapi.user.email;

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
import ru.auto.tests.publicapi.model.VertisPassportChangeEmailParameters;
import ru.auto.tests.publicapi.model.VertisPassportChangeEmailParametersConfirmationCode;
import ru.auto.tests.publicapi.model.VertisPassportUserIdentity;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 31.08.18.
 */

@DisplayName("POST /user/email/change")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class EmailChangeTest {
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
        api.userEmail().changeEmail().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        api.userEmail().changeEmail().body(new VertisPassportChangeEmailParameters())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        api.userEmail().changeEmail().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSeeSuccessChangeEmail() {
        Account account = am.create();
        String email = Utils.getRandomEmail();
        String newEmail = Utils.getRandomEmail();
        String sessionId = adaptor.login(account).getSession().getId();
        account.setLogin(email);

        adaptor.createEmailChangeRequest(sessionId, email);
        String code = adaptor.getEmailCode(account.getId(), 0);

        api.userEmail().changeEmail().body(new VertisPassportChangeEmailParameters().email(newEmail)
                .confirmationCode(new VertisPassportChangeEmailParametersConfirmationCode().code(code).identity(new VertisPassportUserIdentity().email(email))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }
}