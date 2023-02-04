package ru.auto.tests.publicapi.user.profile;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.VertisPassportAutoruUserProfilePatch;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.io.File;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withJsonBody;

/**
 * Created by dskuznetsov on 05.09.18.
 */

@DisplayName("POST /user/profile")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class UserUpdateProfileTest {
    private static final String PHOTO_PATH = "photo/photo.jpg";
    private static final String CONTROL_NAME = "file";
    private static final String USER_PROFILE_PATH = "userProfile/userProfile.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userProfile().updateUserProfile().body(new VertisPassportAutoruUserProfilePatch()).executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WhenNoSession() {
        api.userProfile().updateUserProfile().body(new VertisPassportAutoruUserProfilePatch())
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        api.userProfile().updateUserProfile().xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithNotUniqueAlias() {
        Account account = am.create();
        Account secondAccount = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String secondSessionId = adaptor.login(secondAccount).getSession().getId();
        String alias = Utils.getRandomString();

        api.userProfile().updateUserProfile().xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .body(new VertisPassportAutoruUserProfilePatch().alias(alias))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        AutoApiErrorResponse response = api.userProfile().updateUserProfile().xSessionIdHeader(secondSessionId).reqSpec(defaultSpec())
                .body(new VertisPassportAutoruUserProfilePatch().alias(alias))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);

        AutoruApiModelsAssertions.assertThat(response).hasDetailedError("ALIAS_IS_NOT_UNIQUE");
    }

    @Test
    public void shouldUpdateUserProfile() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String uploadUrl = api.userProfile().userpicUploadUri().xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_OK))).getUri();
        String photoMessage = uploadImage(uploadUrl, PHOTO_PATH);
        String body = format(getResourceAsString(USER_PROFILE_PATH), getRandomString(), photoMessage);

        api.userProfile().updateUserProfile().reqSpec(withJsonBody(body))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    private File getFile(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }

    public String uploadImage(String uploadImage, String photoPath) {
        return RestAssured.given().config(RestAssuredConfig.config()
                .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation()))
                .filter(new AllureLoggerFilter()).contentType("multipart/form-data")
                .multiPart(CONTROL_NAME, getFile(photoPath), "image/jpeg")
                .post(uploadImage).as(JsonObject.class, GSON).get("name").getAsString();
    }
}