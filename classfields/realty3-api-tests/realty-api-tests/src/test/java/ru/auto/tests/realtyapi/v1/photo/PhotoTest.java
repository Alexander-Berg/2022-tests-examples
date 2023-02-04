package ru.auto.tests.realtyapi.v1.photo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.function.Consumer;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("PUT /photo.json")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class PhotoTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private OAuth oAuth;

    @Test
    @Ignore
    public void shouldSee401WithoutFile() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.photo().photoRoute().reqSpec(photoSpec()).authorizationHeader(token)
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    private File getFile(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }

    private static Consumer<RequestSpecBuilder> photoSpec() {
        return authSpec().andThen(r -> r.setContentType("multipart/form-data"));
    }
}
