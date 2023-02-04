package ru.auto.tests.realtyapi.v1.phone;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiPhoneBindRequest;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe400PhoneBadFormat;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe400PhoneBadNumFormat;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.invalidPhones;



@Title("POST /user/check/phone")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CheckPhoneInvalidPhoneTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Parameter
    @Parameterized.Parameter(0)
    public String phone;

    @Parameterized.Parameters(name = "phone={0}")
    public static Object[] getParameters() {
        return invalidPhones();
    }

    @Test
    public void shouldSee400WithInvalidPhone() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.passport().phoneBind().reqSpec(authSpec())
                .body(getPhoneBindRequest(phone))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe400PhoneBadNumFormat()));
    }

    private RealtyApiPhoneBindRequest getPhoneBindRequest(String phone) {
        RealtyApiPhoneBindRequest r = random(RealtyApiPhoneBindRequest.class);
        r.setPhone(phone);
        return r;
    }

}
