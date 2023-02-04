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

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("PUT /photo.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PhotoUpdateTest {
    private static final String IMAGE_JPG_PATH = "photo/image.jpg";
    private static final String IMAGE_PNG_PATH = "photo/image.png";
    private static final String IMAGE_GIF_PATH = "photo/image.gif";
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
    public String mimeType;

    @Parameter
    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "mimeType={0} path={1}")
    public static Object[][] getParameters() {
        return new String[][]{
                {"image/gif", IMAGE_GIF_PATH},
                {"image/jpeg", IMAGE_JPG_PATH},
                {"image/png", IMAGE_PNG_PATH}
        };
    }

    @Test
    public void shouldUpdatePhoto() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.photo().photoRoute().reqSpec(photoSpec()).authorizationHeader(token)
                .reqSpec(req -> req.addMultiPart(CONTROL_NAME, getFile(path), mimeType))
                .execute(validatedWith(shouldBe200Ok()));
        //todo: добавить проверки
    }

    private File getFile(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }

    private static Consumer<RequestSpecBuilder> photoSpec() {
        return authSpec().andThen(r -> r.setContentType("multipart/form-data"));
    }
}