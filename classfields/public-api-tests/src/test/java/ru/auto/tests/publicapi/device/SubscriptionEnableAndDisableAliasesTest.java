package ru.auto.tests.publicapi.device;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.enums.DeviceSubscriptionAlias;
import ru.auto.tests.publicapi.model.AutoApiDevice;
import ru.auto.tests.publicapi.model.AutoApiDeviceSubscription;
import ru.auto.tests.publicapi.model.AutoApiDeviceSubscriptionsResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.stream.Collectors;

import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.enums.DeviceSubscriptionAlias.CHAT_EVENTS;
import static ru.auto.tests.publicapi.enums.DeviceSubscriptionAlias.OFFER_CHANGED;
import static ru.auto.tests.publicapi.enums.DeviceSubscriptionAlias.PERSONAL_RECOMMENDATIONS;
import static ru.auto.tests.publicapi.enums.DeviceSubscriptionAlias.SERVICES_AND_DISCOUNTS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomHelloRequest;


@DisplayName("POST and DELETE /device/subscriptions/{name}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SubscriptionEnableAndDisableAliasesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter("Алиас")
    @Parameterized.Parameter(0)
    public DeviceSubscriptionAlias subscriptionAlias;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(subscriptionAliases());
    }

    private static Object[] subscriptionAliases() {
        return new DeviceSubscriptionAlias[]{
                OFFER_CHANGED,
                CHAT_EVENTS,
                PERSONAL_RECOMMENDATIONS,
                SERVICES_AND_DISCOUNTS
        };
    }

    @Test
    @Owner(TIMONDL)
    public void shouldEnableDeviceSubscription() {
        String deviceId = createAndRegisterDeviceId();

        disableDeviceSubscription(deviceId, subscriptionAlias);
        enableDeviceSubscription(deviceId, subscriptionAlias);

        AutoApiDeviceSubscription subscription = getDeviceSubscriptionByAlias(deviceId, subscriptionAlias);

        assertThat(subscription).hasIsEnabled(true);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldDisableDeviceSubscription() {
        String deviceId = createAndRegisterDeviceId();

        disableDeviceSubscription(deviceId, subscriptionAlias);

        AutoApiDeviceSubscription subscription = getDeviceSubscriptionByAlias(deviceId, subscriptionAlias);

        assertThat(subscription).hasIsEnabled(false);
    }

    @Step("Создаем и регистрируем DeviceID")
    public String createAndRegisterDeviceId() {
        return api.device().hello().body(getRandomHelloRequest(AutoApiDevice.PlatformEnum.ANDROID)).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()))
                .getHeader("x-device-uid");
    }

    @Step("Деактивируем подписку «{subscriptionAlias}» у девайса с id «{deviceId}»")
    public void disableDeviceSubscription(String deviceId, DeviceSubscriptionAlias subscriptionAlias) {
        api.deviceSubscriptions().disableDeviceSubscription().reqSpec(defaultSpec())
                .namePath(subscriptionAlias.value())
                .xDeviceUidHeader(deviceId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Активируем подписку «{subscriptionAlias}» у девайса с id «{deviceId}»")
    public void enableDeviceSubscription(String deviceId, DeviceSubscriptionAlias subscriptionAlias) {
        api.deviceSubscriptions().enableDeviceSubscription().reqSpec(defaultSpec())
                .namePath(subscriptionAlias.value())
                .xDeviceUidHeader(deviceId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Получаем информацию о подписке «{subscriptionAlias}» для девайса с id «{deviceId}»")
    public AutoApiDeviceSubscription getDeviceSubscriptionByAlias(String deviceId, DeviceSubscriptionAlias subscriptionAlias) {
        AutoApiDeviceSubscriptionsResponse subscriptionsResponse = api.deviceSubscriptions().deviceSubscriptions().reqSpec(defaultSpec())
                .xDeviceUidHeader(deviceId)
                .execute(validatedWith(shouldBeSuccess()))
                .as(AutoApiDeviceSubscriptionsResponse.class);

        return subscriptionsResponse.getSubscriptions()
                .stream()
                .filter(deviceSubscription -> deviceSubscription.getAlias().equalsIgnoreCase(subscriptionAlias.value()))
                .collect(Collectors.toList())
                .get(0);
    }

}
