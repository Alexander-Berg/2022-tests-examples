package ru.auto.tests.realtyapi.v1.user;


import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import org.assertj.Assertions;
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
import ru.auto.tests.realtyapi.responses.UpdateExplicitUserResp;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.VosUserModel;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.Lists.newArrayList;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.model.VosUserModel.PaymentTypeEnum.NATURAL_PERSON;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultUserType;


@Title("PUT /user/{uid}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class EditUserWithExplicitUidDifferentUserTypeTest {
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
    public Integer userType;

    @Parameterized.Parameters(name = "userType={0}")
    public static Object[] getParameters() {
        return defaultUserType();
    }

    @Test
    public void shouldEditUserWithDifferentUserType() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        VosUserModel reqBody = getBodyRequest(account.getId());

        UpdateExplicitUserResp resp = api.user().updateUserRoute().reqSpec(authSpec()).authorizationHeader(token)
                .uidPath(account.getId())
                .body(reqBody.type(userType))
                .execute(validatedWith(shouldBe200OkJSON())).as(UpdateExplicitUserResp.class, GSON);

        Assertions.assertThat(resp).hasType(reqBody.getType().longValue());
    }

    private VosUserModel getBodyRequest(String uid) {
        return random(VosUserModel.class).login(uid).type(0).email("test@yandex.q")
                .paymentType(NATURAL_PERSON)
                .allowedCommunicationChannels(newArrayList("COM_CALLS"));
    }
}
