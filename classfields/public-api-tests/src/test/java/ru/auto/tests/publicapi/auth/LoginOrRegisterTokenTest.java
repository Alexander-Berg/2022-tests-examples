package ru.auto.tests.publicapi.auth;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.JSON;
import ru.auto.tests.publicapi.adaptor.PushnoyAdaptor;
import ru.auto.tests.publicapi.config.PublicApiConfig;
import ru.auto.tests.publicapi.model.VertisPassportLoginOptions;
import ru.auto.tests.publicapi.model.VertisPassportLoginOrRegisterParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static java.lang.System.getProperties;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_LOGIN;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe403ClientLoginNotAllowedError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe403PasswordRequiredError;
import static ru.auto.tests.publicapi.testdata.TestData.provideToken;

@DisplayName("POST /auth/login-or-register")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class LoginOrRegisterTokenTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PushnoyAdaptor adaptor;

    @Parameter("Токен")
    @Parameterized.Parameter
    public String token;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(provideToken());

    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeePassAuthRequiredWithDealer() {

        VertisPassportLoginOrRegisterParameters params = new VertisPassportLoginOrRegisterParameters()
                .email(DEALER_LOGIN)
                .options(new VertisPassportLoginOptions().allowClientLogin(true))
                .suppressNotifications(false);
        String tamper = adaptor.getTamperForRequest(params, "");

        api.auth().loginOrRegister()
                .xAuthorizationHeader(token)
                .xTimestampHeader(tamper)
                .body(params)
                .executeAs(validatedWith(shouldBe403PasswordRequiredError()));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeeClientLoginNowAllowedWithoutOption() {

        VertisPassportLoginOrRegisterParameters params = new VertisPassportLoginOrRegisterParameters()
                .email(DEALER_LOGIN)
                .suppressNotifications(false);
        String tamper = adaptor.getTamperForRequest(params, "");

        api.auth().loginOrRegister()
                .xAuthorizationHeader(token)
                .xTimestampHeader(tamper)
                .body(params)
                .executeAs(validatedWith(shouldBe403ClientLoginNotAllowedError()));
    }
}
