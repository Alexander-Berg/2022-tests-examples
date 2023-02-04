package ru.auto.tests.realtyapi.v1.device;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.RegisterDeviceRequest;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getEmptyBody;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("POST /device/hello")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class PostDeviceHello {

    private static final String VALID_OS = "ios";

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

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.device().helloRoute()
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithNoBody() {
        api.device().helloRoute().reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee200WithEmptyBody() {
        api.device().helloRoute().reqSpec(authSpec())
                .reqSpec(r -> r.setBody(getEmptyBody()))
                .execute(validatedWith(shouldBe200Ok()));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidOS() {
        api.device().helloRoute().reqSpec(authSpec())
                .body(random(RegisterDeviceRequest.class))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee200WithValidOS() {
        api.device().helloRoute().reqSpec(authSpec())
                .body(random(RegisterDeviceRequest.class)
                        .clientOS(VALID_OS))
                .execute(validatedWith(shouldBe200Ok()));
    }
}
