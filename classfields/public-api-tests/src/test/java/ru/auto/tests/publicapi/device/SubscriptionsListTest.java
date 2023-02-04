package ru.auto.tests.publicapi.device;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiDevice;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe404DeviceNotFound;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomHelloRequest;


@DisplayName("GET /device/subscriptions")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class SubscriptionsListTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    @Owner(TIMONDL)
    public void shouldSee404WhenNoDeviceId() {
        api.deviceSubscriptions().deviceSubscriptions().reqSpec(defaultSpec()).execute(validatedWith(shouldBe404DeviceNotFound()));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeListOfDeviceSubscriptions() {
        String deviceId = api.device().hello().body(getRandomHelloRequest(AutoApiDevice.PlatformEnum.ANDROID)).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()))
                .getHeader("x-device-uid");

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.deviceSubscriptions().deviceSubscriptions().reqSpec(defaultSpec())
                .xDeviceUidHeader(deviceId)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

}
