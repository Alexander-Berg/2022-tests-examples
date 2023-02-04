package ru.auto.tests.publicapi.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiDevice;
import ru.auto.tests.publicapi.model.AutoApiHelloRequest;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.HashMap;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeEmptyJson;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 20.08.18
 */


@DisplayName("GET /offer/{category}/{offerID}/notifications")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetNotificationsTest {
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
        api.offerCard().notificationStats().categoryPath(CARS)
                .offerIDPath(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSeeSuccessWhenNoData() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String deviceUid = createAndRegisterDeviceId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        api.offerCard().notificationStats().categoryPath(CARS).offerIDPath(offerId).reqSpec(defaultSpec())
                .xDeviceUidHeader(deviceUid).execute(validatedWith(shouldBeEmptyJson()));

    }

    @Step("Создаем и регистрируем DeviceID с «notification_experiment»: «disabled»")
    public String createAndRegisterDeviceId() {
        AutoApiHelloRequest hr = random(AutoApiHelloRequest.class, "supportedFeatures", "remote_config");
        hr.getDevice().setPlatform(AutoApiDevice.PlatformEnum.ANDROID);
        hr.setRemoteConfig(new HashMap<String, String>() {{
            put("notification_experiment", "disabled");
        }});

        return api.device().hello().body(hr).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()))
                .getHeader("x-device-uid");
    }
}
