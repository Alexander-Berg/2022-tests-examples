package ru.auto.tests.publicapi.device;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiDevice.PlatformEnum;
import ru.auto.tests.publicapi.model.AutoApiSavePushTokenRequest;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiDevice.PlatformEnum.ANDROID;
import static ru.auto.tests.publicapi.model.AutoApiDevice.PlatformEnum.IOS;
import static ru.auto.tests.publicapi.model.AutoApiSavePushTokenRequest.PushTokenTypeEnum.FIREBASE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 15.09.17.
 */

@DisplayName("POST /device/push-token")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PushTokenPlatformTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter("Платформа")
    @Parameterized.Parameter(0)
    public PlatformEnum platform;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(platforms());
    }


    private static Object[] platforms() {
        return new PlatformEnum[]{
                ANDROID,
                IOS
        };
    }

    @Test
    public void shouldSeeSuccess() {
        api.device().pushToken().reqSpec(defaultSpec()).body(getRandomRequest(platform))
                .execute(validatedWith(shouldBeSuccess()));
    }

    private static AutoApiSavePushTokenRequest getRandomRequest(PlatformEnum platform) {
        AutoApiSavePushTokenRequest hr = random(AutoApiSavePushTokenRequest.class);
        hr.getDevice().setPlatform(platform);
        hr.setPushTokenType(FIREBASE);
        return hr;
    }
}
