package ru.auto.tests.publicapi.user;

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
import ru.auto.tests.publicapi.model.VertisPassportConfirmIdentityParameters;
import ru.auto.tests.publicapi.model.VertisPassportConfirmIdentityResult;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Created by dskuznetsov on 31.08.18.
 */

@DisplayName("POST /user/confirm")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class UserConfirmTest {
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
        api.user().confirmIdentity().body(new VertisPassportConfirmIdentityParameters()).executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        api.user().confirmIdentity().body(new VertisPassportConfirmIdentityParameters())
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldChangeUserEmail() {
        Account account = am.create();
        String email = Utils.getRandomEmail();
        String newEmail = Utils.getRandomEmail();
        String sessionId = adaptor.login(account).getSession().getId();
        account.setLogin(email);

        adaptor.createEmailChangeRequest(sessionId, email);
        String code = adaptor.getEmailCode(account.getId(), 0);

        adaptor.confirmEmailChangeRequest(sessionId, code, email, newEmail);
        String secondCode = adaptor.getEmailCode(account.getId(), 1);

        VertisPassportConfirmIdentityResult response = api.user().confirmIdentity().body(new VertisPassportConfirmIdentityParameters().code(secondCode).email(newEmail))
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.getUser().getEmails().get(0).getEmail()).isEqualTo(newEmail);
    }
}