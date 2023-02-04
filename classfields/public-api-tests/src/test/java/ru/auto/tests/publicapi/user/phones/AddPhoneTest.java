package ru.auto.tests.publicapi.user.phones;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiAddIdentityResponse;
import ru.auto.tests.publicapi.model.AutoApiAddIdentityResponseAssert;
import ru.auto.tests.publicapi.model.VertisPassportAddPhoneParameters;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiAddIdentityResponse.StatusEnum.SUCCESS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /user/phones")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class AddPhoneTest {

    private static final int CODE_LENGTH = 4;

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
        api.userPhones().addPhone().body(new VertisPassportAddPhoneParameters()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutPhone() {
        AutoApiErrorResponse response = api.userPhones().addPhone().body(new VertisPassportAddPhoneParameters())
                .reqSpec(defaultSpec()).execute(ResponseSpecBuilders.validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasError(AutoApiErrorResponse.ErrorEnum.BAD_REQUEST).hasStatus(AutoApiErrorResponse.StatusEnum.ERROR).hasDetailedError("requirement failed: Phone is required");
    }

    @Test
    public void shouldSee409AddForeignPhoneWithConfirmed() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        Account secondAccount = am.create();

        AutoApiErrorResponse response = api.userPhones().addPhone().reqSpec(defaultSpec()).xSessionIdHeader(sessionId).body(new VertisPassportAddPhoneParameters()
                .phone(secondAccount.getPhone().get()).confirmed(true)).execute(validatedWith(shouldBeCode(SC_CONFLICT))).as(AutoApiErrorResponse.class);

        AutoApiErrorResponseAssert.assertThat(response).hasStatus(AutoApiErrorResponse.StatusEnum.ERROR)
                .hasError(AutoApiErrorResponse.ErrorEnum.IDENTITY_LINKED_TO_OTHER_USER)
                .hasDetailedError("Phone/email already linked to other user");
    }

    @Test
    public void shouldAddForeignPhone() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        Account secondAccount = am.create();

        AutoApiAddIdentityResponse response = api.userPhones().addPhone().reqSpec(defaultSpec()).xSessionIdHeader(sessionId).body(new VertisPassportAddPhoneParameters()
                .phone(secondAccount.getPhone().get()).confirmed(false)).executeAs(validatedWith(shouldBeSuccess()));

        AutoApiAddIdentityResponseAssert.assertThat(response).hasStatus(SUCCESS).hasNeedConfirm(true).hasCodeLength(CODE_LENGTH);
    }
}
