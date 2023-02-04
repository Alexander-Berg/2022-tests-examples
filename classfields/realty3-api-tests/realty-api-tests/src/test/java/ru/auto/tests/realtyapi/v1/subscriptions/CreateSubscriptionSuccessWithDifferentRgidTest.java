package ru.auto.tests.realtyapi.v1.subscriptions;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.SubscriptionResponse;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.SubscriptionRequest;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getValidSubscriptionRequest;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.validRgids;



@Title("POST /user/subscriptions")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Log4j
@Ignore("HTTP status 409")
public class CreateSubscriptionSuccessWithDifferentRgidTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Parameter
    @Parameterized.Parameter(0)
    public long rgid;

    @Parameterized.Parameters(name = "rgid={0}")
    public static Object[] getParameters() {
        return validRgids();
    }

    @Test
    public void shouldSuccessCreateSubscriptionWithDifferentRgid() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        SubscriptionRequest reqBody = getValidSubscriptionRequest();
        reqBody.getQuery().setRgid(rgid);

        SubscriptionResponse resp = api.subscriptions().createSubscriptionRoute().reqSpec(authSpec()).authorizationHeader(token)
                .body(reqBody)
                .execute(validatedWith(shouldBe200OkJSON())).as(SubscriptionResponse.class, GSON);

        adaptor.getSubscriptionById(resp.getResponse().getId());
    }
}
