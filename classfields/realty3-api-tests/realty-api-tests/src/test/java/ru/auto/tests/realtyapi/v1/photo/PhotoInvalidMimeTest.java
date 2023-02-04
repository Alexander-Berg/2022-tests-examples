package ru.auto.tests.realtyapi.v1.photo;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.restassured.builder.RequestSpecBuilder;
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
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.function.Consumer;

import static io.restassured.http.ContentType.BINARY;
import static io.restassured.http.ContentType.TEXT;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;



@Title("PUT /photo.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PhotoInvalidMimeTest {
    private static final String IMAGE_JPG_PATH = "photo/image.jpg";
    private static final String CONTROL_NAME = "file";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private OAuth oAuth;

    @Parameter
    @Parameterized.Parameter(0)
    public String invalidMimeType;

    @Parameterized.Parameters(name = "invalidMimeType={0}")
    public static String[] getParameters() {
        return new String[]{
                TEXT.name(),
                BINARY.name(),
                getRandomString()
        };
    }

    @Test
    public void shouldNotUpdatePhotoWithInvalidMimeType() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.photo().photoRoute().reqSpec(photoSpec()).authorizationHeader(token)
                .reqSpec(req -> req.addMultiPart(CONTROL_NAME, getFile(IMAGE_JPG_PATH), invalidMimeType))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }


    private File getFile(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }

    private static Consumer<RequestSpecBuilder> photoSpec() {
        return authSpec().andThen(r -> r.setContentType("multipart/form-data"));
    }
}
