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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.model.CreateUserRequest;
import ru.auto.tests.realty.vos2.model.RequestInfo;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.CreateUserErrorResp;
import ru.auto.tests.realty.vos2.objects.Error;
import ru.auto.tests.realty.vos2.objects.GetUserResp;
import ru.auto.tests.realty.vos2.testdata.TestData;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.enums.ErrorStatusCode.USER_OR_PHONE_ALREADY_EXISTS;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.PaymentTypeEnum;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.SERIALIZED_NAME_LICENSE_AGREEMENT;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.SERIALIZED_NAME_LOGIN;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.SERIALIZED_NAME_PAYMENT_TYPE;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.SERIALIZED_NAME_REDIRECT_PHONES;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithUnableToParseParam;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getObjectFromJson;

@DisplayName("POST /api/realty/user/create")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class CreateUserTest {

    private static final String USER_REQUEST_BODY_PATH = "testdata/user_request_body.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient vos2;

    @Inject
    private Account account;

    @Test
    public void shouldSee400WithoutBody() {
        vos2.user().createRoute().reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        vos2.user().createRoute().body(new CreateUserRequest()).execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee400WithInvalidId() {
        String randomLogin = Utils.getRandomString();
        vos2.user().createRoute().body(getUser().login(randomLogin))
                .execute(validatedWith(shouldBe400WithUnableToParseParam("login", randomLogin)));
    }

    @Test
    public void shouldSee400WithInvalidEmail() {
        String randomEmail = Utils.getRandomString();
        vos2.user().createRoute().body(getUser().login(account.getId()).email(randomEmail))
                .execute(validatedWith(shouldBe400WithUnableToParseParam("email", randomEmail)));
    }

    @Test
    public void shouldSee400WithInvalidPaymentType() {
        String randomPaymentType = Utils.getRandomString();
        JsonObject body = getBody();
        body.addProperty(SERIALIZED_NAME_LOGIN, account.getId());
        body.addProperty(SERIALIZED_NAME_PAYMENT_TYPE, randomPaymentType);

        vos2.user().createRoute().reqSpec(r -> r.setContentType(JSON).setBody(body))
                .execute(validatedWith(shouldBe400WithUnableToParseParam("paymentType", randomPaymentType)));
    }

    @Test
    public void shouldSee400WithInvalidBooleanValue() {
        JsonObject body = getBody();
        body.addProperty(SERIALIZED_NAME_LOGIN, account.getId());
        body.addProperty(SERIALIZED_NAME_LICENSE_AGREEMENT, Utils.getRandomString());
        body.addProperty(SERIALIZED_NAME_REDIRECT_PHONES, Utils.getRandomString());

        vos2.user().createRoute().reqSpec(r -> r.setContentType(JSON)
                .setBody(body)).execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @DataProvider
    public static Object[] paymentTypes() {
        return TestData.defaultPaymentTypes();
    }

    @Test
    @UseDataProvider("paymentTypes")
    public void shouldSuccessCreateUserWithPaymentTypes(PaymentTypeEnum paymentType) {
        vos2.user().createRoute().body(getUser().login(account.getId())
                .paymentType(paymentType))
                .execute(validatedWith(shouldBeStatusOk()));

        GetUserResp resp = vos2.user().getUserRoute().userIDPath(account.getId())
                .execute(validatedWith(shouldBeStatusOk())).as(GetUserResp.class, GSON);

        Assertions.assertThat(resp.getUser()).hasPaymentType(paymentType.getValue());
    }

    @DataProvider
    public static Object[] userTypes() {
        return TestData.defaultUserTypes();
    }

    @Test
    @UseDataProvider("userTypes")
    public void shouldSuccessCreateUserWithTypes(CreateUserRequest.TypeEnum type) {
        vos2.user().createRoute()
                .body(getUser().login(account.getId()).type(type))
                .execute(validatedWith(shouldBeStatusOk()));

        GetUserResp resp = vos2.user().getUserRoute().userIDPath(account.getId())
                .execute(validatedWith(shouldBeStatusOk()))
                .as(GetUserResp.class, GSON);

        Assertions.assertThat(resp.getUser()).hasType(type.getValue().longValue());
    }

    @Test
    public void shouldSee409WhenTwiceCreateUserWithSameId() {
        vos2.user().createRoute().body(getUser().login(account.getId()))
                .execute(validatedWith(shouldBeStatusOk()));
        CreateUserErrorResp resp = vos2.user().createRoute().body(getUser().login(account.getId()))
                .execute(validatedWith(shouldBeCode(SC_CONFLICT))).as(CreateUserErrorResp.class, GSON);

        Assertions.assertThat(resp).hasErrors(new Error().withCode(USER_OR_PHONE_ALREADY_EXISTS.value())
                .withDescription(format("User uid_%s already exists", account.getId())));
    }

    @Test
    public void shouldSuccessCreateUser() {
        CreateUserRequest user = getUser();

        vos2.user().createRoute().body(user.login(account.getId()).request(random(RequestInfo.class)))
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
                .hasPhones(user.getTelephones().get(0))
//                .hasLicenseAgreement(user.isLicenseAgreement())
//                .hasRedirectPhones(user.isRedirectPhones())
                .hasPaymentType(user.getPaymentType().getValue());
    }

    private CreateUserRequest getUser() {
        return getObjectFromJson(CreateUserRequest.class, USER_REQUEST_BODY_PATH);
    }

    private JsonObject getBody() {
        return getObjectFromJson(JsonObject.class, USER_REQUEST_BODY_PATH);
    }
}
