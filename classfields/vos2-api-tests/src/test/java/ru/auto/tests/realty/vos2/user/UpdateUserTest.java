package ru.auto.tests.realty.vos2.user;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.StringUtils;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.model.CreateUserRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.Error;
import ru.auto.tests.realty.vos2.objects.GetUserResp;
import ru.auto.tests.realty.vos2.objects.UpdateUserErrorResp;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.enums.ErrorStatusCode.ILLEGAL_ARGUMENT;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.PaymentTypeEnum;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.SERIALIZED_NAME_LICENSE_AGREEMENT;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.SERIALIZED_NAME_LOGIN;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.SERIALIZED_NAME_PAYMENT_TYPE;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.SERIALIZED_NAME_REDIRECT_PHONES;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithUnableToParseParam;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultPaymentTypes;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultUserTypes;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getObjectFromJson;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("PUT /api/realty/user/update/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdateUserTest {

    private static final String USER_REQUEST_BODY_PATH = "testdata/user_request_body.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient vos2;

    @Inject
    @Vos
    private Account account;

    @Test
    public void shouldSee400WithoutBody() {
        vos2.user().updateRoute().userIDPath(account.getId()).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        String emptyBody = "{}";
        vos2.user().updateRoute().userIDPath(account.getId()).reqSpec(jsonBody(emptyBody))
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404WithRandomVosID() {
        String randomVosId = getRandomString();
        vos2.user().updateRoute().userIDPath(randomVosId).body(getUserRequest().login(account.getId()))
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee400WithInvalidLogin() {
        String randomLogin = getRandomString();
        vos2.user().updateRoute().userIDPath(account.getId()).body(getUserRequest().login(randomLogin))
                .execute(validatedWith(shouldBe400WithUnableToParseParam("login", randomLogin)));
    }

    @Test
    public void shouldSee400WithRandomLogin() {
        String randomLogin = getRandomLogin();
        UpdateUserErrorResp resp = vos2.user().updateRoute().userIDPath(account.getId())
                .body(getUserRequest().login(randomLogin))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(UpdateUserErrorResp.class, GSON);

        Assertions.assertThat(resp).hasErrors(new Error().withCode(ILLEGAL_ARGUMENT.value())
                .withDescription("Login cannot be changed"));
    }

    @Test
    public void shouldSee400WithRandomEmail() {
        String randomEmail = getRandomString();
        vos2.user().updateRoute().userIDPath(account.getId())
                .body(getUserRequest().login(account.getId()).email(randomEmail))
                .execute(validatedWith(shouldBe400WithUnableToParseParam("email", randomEmail)));
    }

    @Test
    public void shouldSee400WithInvalidBooleanValue() {
        JsonObject body = getUserRequestJson();
        body.addProperty(SERIALIZED_NAME_LOGIN, account.getId());
        body.addProperty(SERIALIZED_NAME_LICENSE_AGREEMENT, getRandomString());
        body.addProperty(SERIALIZED_NAME_REDIRECT_PHONES, getRandomString());

        vos2.user().updateRoute().userIDPath(account.getId()).reqSpec(r -> r.setContentType(JSON).setBody(body))
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee400WithInvalidPaymentType() {
        String invalidPaymentType = getRandomString();
        JsonObject body = getUserRequestJson();
        body.addProperty(SERIALIZED_NAME_LOGIN, account.getId());
        body.addProperty(SERIALIZED_NAME_PAYMENT_TYPE, invalidPaymentType);

        vos2.user().updateRoute().userIDPath(account.getId()).reqSpec(r -> r.setContentType(JSON).setBody(body))
                .execute(validatedWith(shouldBe400WithUnableToParseParam("paymentType", invalidPaymentType)));
    }

    @Test
    public void shouldSuccessUpdateWithRandomName() {
        String randomName = getRandomString();
        vos2.user().updateRoute().userIDPath(account.getId())
                .body(getUserRequest().login(account.getId()).name(randomName))
                .execute(validatedWith(shouldBeStatusOk())).as(UpdateUserErrorResp.class, GSON);

        GetUserResp resp = vos2.user().getUserRoute().userIDPath(account.getId())
                .execute(validatedWith(shouldBeStatusOk())).as(GetUserResp.class, GSON);

        Assertions.assertThat(resp.getUser()).hasName(randomName);
    }

    @Test
    public void shouldSuccessUpdateUser() {
        CreateUserRequest user = getObjectFromJson(CreateUserRequest.class,
                "testdata/user_request_test_body.json");

        vos2.user().updateRoute().userIDPath(account.getId()).body(user.login(account.getId()))
                .execute(validatedWith(shouldBeStatusOk()));

        GetUserResp resp = vos2.user().getUserRoute().userIDPath(account.getId())
                .execute(validatedWith(shouldBeStatusOk()))
                .as(GetUserResp.class, GSON);

        Assertions.assertThat(resp.getUser())
                .hasLogin(user.getLogin())
                .hasType(user.getType().getValue().longValue())
                .hasEmail(user.getEmail())
                .hasName(user.getName())
                .hasOrganization(user.getOrganization())
                .hasOgrn(user.getOgrn())
                .hasAgencyId(user.getAgencyId())
//                .hasCallCenter(user.isCallCenter())
                .hasUrl(user.getUrl())
                .hasPhotoUrl(user.getPhotoUrl())
                .hasUrl(user.getUrl())
                .hasPhones(user.getTelephones().get(0), user.getTelephones().get(1))
//                .hasLicenseAgreement(user.isLicenseAgreement())
//                .hasRedirectPhones(user.isRedirectPhones())
                .hasPaymentType(user.getPaymentType().getValue());
    }

    @DataProvider
    public static Object[] userTypes() {
        return defaultUserTypes();
    }

    @Test
    @UseDataProvider("userTypes")
    public void shouldSuccessUpdateUserWithDifferentTypes(CreateUserRequest.TypeEnum type) {
        vos2.user().updateRoute().userIDPath(account.getId())
                .body(getUserRequest().login(account.getId()).type(type))
                .execute(validatedWith(shouldBeStatusOk()));

        GetUserResp resp = vos2.user().getUserRoute().userIDPath(account.getId())
                .execute(validatedWith(shouldBeStatusOk()))
                .as(GetUserResp.class, GSON);

        Assertions.assertThat(resp.getUser()).hasType(type.getValue().longValue());
    }

    @DataProvider
    public static Object[] paymentTypes() {
        return defaultPaymentTypes();
    }

    @Test
    @UseDataProvider("paymentTypes")
    public void shouldSuccessUpdateUserWithPaymentType(PaymentTypeEnum paymentType) {
        vos2.user().updateRoute().userIDPath(account.getId()).body(getUserRequest().login(account.getId())
                .paymentType(paymentType))
                .execute(validatedWith(shouldBeStatusOk()));

        GetUserResp resp = vos2.user().getUserRoute().userIDPath(account.getId())
                .execute(validatedWith(shouldBeStatusOk())).as(GetUserResp.class, GSON);

        Assertions.assertThat(resp.getUser()).hasPaymentType(paymentType.getValue());
    }

    private CreateUserRequest getUserRequest() {
        return getObjectFromJson(CreateUserRequest.class, USER_REQUEST_BODY_PATH);
    }

    private JsonObject getUserRequestJson() {
        return getObjectFromJson(JsonObject.class, USER_REQUEST_BODY_PATH);
    }
}
