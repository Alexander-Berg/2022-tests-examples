package ru.auto.tests.publicapi.user.phones;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiAddIdentityResponse;
import ru.auto.tests.publicapi.model.AutoApiAddIdentityResponseAssert;
import ru.auto.tests.publicapi.model.VertisPassportAddPhoneParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiAddIdentityResponse.StatusEnum.SUCCESS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.TestData.trueFalse;


@DisplayName("POST /user/phones")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddPhoneConfirmedTest {

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

    @Parameter("Подтвержден ли номер")
    @Parameterized.Parameter(0)
    public boolean confirmed;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(trueFalse());
    }

    @Test
    public void shouldAddRandomPhoneWithAnonymSession() {
        String phone = Utils.getRandomPhone();
        String sessionId = adaptor.session().getSession().getId();

        AutoApiAddIdentityResponse response = api.userPhones().addPhone().body(new VertisPassportAddPhoneParameters().phone(phone)
                .confirmed(confirmed)).xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));
        AutoApiAddIdentityResponseAssert.assertThat(response).hasStatus(SUCCESS).hasNeedConfirm(true).hasCodeLength(CODE_LENGTH);
    }


    @Test
    public void shouldAddRandomPhoneWithoutSessionId() {
        String phone = Utils.getRandomPhone();
        AutoApiAddIdentityResponse response = api.userPhones().addPhone().body(new VertisPassportAddPhoneParameters().phone(phone).confirmed(confirmed))
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));
        AutoApiAddIdentityResponseAssert.assertThat(response).hasStatus(SUCCESS).hasNeedConfirm(true).hasCodeLength(CODE_LENGTH);
    }

    @Test
    public void shouldAddPhoneTwice() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiAddIdentityResponse response = api.userPhones().addPhone().reqSpec(defaultSpec()).xSessionIdHeader(sessionId).body(new VertisPassportAddPhoneParameters()
                .phone(account.getPhone().get()).confirmed(confirmed)).executeAs(validatedWith(shouldBeSuccess()));
        AutoApiAddIdentityResponseAssert.assertThat(response).hasStatus(SUCCESS).hasNeedConfirm(false);
    }
}
