package ru.auto.tests.realtyapi.v2.personalAccount;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
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
import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserInfo.PaymentTypeEnum;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserInfo.UserTypeEnum;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.util.Arrays.stream;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.model.RealtyApiUsersUserInfo.PaymentTypeEnum.UNKNOWN;
import static ru.auto.tests.realtyapi.v2.testdata.UserTestData.getUpdate;

@Title("PUT /user/{uid}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UpdateUserInfoTest {

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
    @Parameterized.Parameter
    public UserTypeEnum type;

    @Parameter
    @Parameterized.Parameter(1)
    public PaymentTypeEnum paymentType;

    @Parameterized.Parameters(name = "type={0} paymentType={1}")
    public static List<Object[]> getParameters() {
        List<Object[]> parameters = new ArrayList<>();

        stream(UserTypeEnum.values())
                .filter(ut -> !ut.equals(UserTypeEnum.VERIFIER))
                .forEach(type -> stream(PaymentTypeEnum.values())
                        .filter(pt -> !pt.equals(UNKNOWN))
                        .forEach(paymentType -> parameters.add(new Object[]{type, paymentType}))
        );

        return parameters;
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUpdateUserInfo() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String uid = getUid(account);

        api.personalAccountUsers().updateUserRoute().reqSpec(authSpec())
                .uidPath(uid)
                .body(getUpdate(type, paymentType))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));

        JsonObject response = api.personalAccountUsers().getUserRoute().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("userInfo");

        Assertions.assertThat(response.get("userType").getAsString())
                .describedAs("Пользователь должен иметь заданный тип")
                .isEqualTo(type.getValue());

        Assertions.assertThat(response.get("paymentType").getAsString())
                .describedAs("Пользователь должен иметь заданный тип оплаты")
                .isEqualTo(paymentType.getValue());
    }
}
