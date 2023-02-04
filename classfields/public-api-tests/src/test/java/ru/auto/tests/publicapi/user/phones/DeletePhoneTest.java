package ru.auto.tests.publicapi.user.phones;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.INVALID_PHONE_NUMBER;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.LAST_IDENTITY_REMOVE_NOT_ALLOWED;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NoAuth;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("DELETE /user/phones/{phone}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeletePhoneTest {

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
        Account account = am.create();
        api.userPhones().removePhone().phonePath(account.getId()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSession() {
        Account account = am.create();
        api.userPhones().removePhone().phonePath(account.getPhone().get())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NoAuth()));
    }

    @Test
    public void shouldSee401ForAnonym() {
        String sessionId = adaptor.session().getSession().getId();
        String phone = Utils.getRandomPhone();

        api.userPhones().removePhone().phonePath(phone).xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NoAuth()));
    }

    @Test
    public void shouldSee400ForInvalidPhone() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String invalidPhone = Utils.getRandomString();
        AutoApiErrorResponse response = api.userPhones().removePhone().phonePath(invalidPhone)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);

        AutoApiErrorResponseAssert.assertThat(response)
                .hasError(INVALID_PHONE_NUMBER)
                .hasDetailedError(INVALID_PHONE_NUMBER.name())
                .hasStatus(ERROR);

    }

    @Test
    public void shouldDeleteRandomPhone() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String phone = Utils.getRandomPhone();
        api.userPhones().removePhone().phonePath(phone)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldNotDeleteLastPhone() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiErrorResponse response = api.userPhones().removePhone().phonePath(account.getPhone().get()).xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response)
                .hasError(LAST_IDENTITY_REMOVE_NOT_ALLOWED)
                .hasDetailedError(LAST_IDENTITY_REMOVE_NOT_ALLOWED.name())
                .hasStatus(ERROR);
    }

    @Test
    @Ignore("планируется")
    public void shouldDeletePhone() {
    }

    @Test
    @Ignore("планируется")
    public void shouldDeleteLastPhoneIfEmailExist() {
    }
}
