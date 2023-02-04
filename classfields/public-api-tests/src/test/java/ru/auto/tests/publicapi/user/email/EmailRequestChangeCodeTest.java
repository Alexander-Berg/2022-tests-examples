package ru.auto.tests.publicapi.user.email;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Java6Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.VertisPassportRequestEmailChangeParameters;
import ru.auto.tests.publicapi.model.VertisPassportRequestEmailChangeResult;
import ru.auto.tests.publicapi.model.VertisPassportUserIdentity;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Issue;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NoAuth;

/**
 * Created by dskuznetsov on 31.08.18.
 */

@DisplayName("POST /user/email/request-change-code")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class EmailRequestChangeCodeTest {
    private static final Integer CODE_LENGTH = 6;

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
        api.userEmail().requestEmailChangeCode().body(new VertisPassportRequestEmailChangeParameters()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        api.userEmail().requestEmailChangeCode().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Issue("AUTORUAPI-4735")
    public void shouldSee400WithoutUserIdentity() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        api.userEmail().requestEmailChangeCode().body(new VertisPassportRequestEmailChangeParameters().currentIdentity(new VertisPassportUserIdentity()))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        api.userEmail().requestEmailChangeCode().body(new VertisPassportRequestEmailChangeParameters())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NoAuth()));
    }

    @Test
    public void shouldSee200WithEmailChangeCodeRequest() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        account.setLogin(Utils.getRandomEmail());

        VertisPassportRequestEmailChangeResult response = api.userEmail().requestEmailChangeCode()
                .body(new VertisPassportRequestEmailChangeParameters().currentIdentity(new VertisPassportUserIdentity().email(account.getLogin())))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_OK)));

        Java6Assertions.assertThat(response.getCodeLength()).isEqualTo(CODE_LENGTH);
    }
}
