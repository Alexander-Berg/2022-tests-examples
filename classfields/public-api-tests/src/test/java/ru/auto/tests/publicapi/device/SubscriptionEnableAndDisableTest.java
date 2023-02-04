package ru.auto.tests.publicapi.device;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.enums.DeviceSubscriptionAlias.CHAT_EVENTS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe404DeviceNotFound;


@DisplayName("POST and DELETE /device/subscriptions/{name}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class SubscriptionEnableAndDisableTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(TIMONDL)
    public void shouldSee404WhenNoDeviceIdOnEnableSubscription() {
        api.deviceSubscriptions().enableDeviceSubscription().namePath(CHAT_EVENTS.value())
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe404DeviceNotFound()));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee404WhenNoDeviceIdOnDisableSubscription() {
        api.deviceSubscriptions().disableDeviceSubscription().namePath(CHAT_EVENTS.value())
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe404DeviceNotFound()));
    }
}
