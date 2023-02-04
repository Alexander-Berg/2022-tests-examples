package ru.auto.tests.publicapi.pushnoy;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.awaitility.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PushnoyAdaptor;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;

import java.util.List;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.DealerConsts.CLIENT_ID;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /auth/logout")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiDealerModule.class)
public class DealerDetachDeviceTest {
    private static final String USER_TYPE = "dealer";
    private static final int POLL_INTERVAL = 2;
    private static final int TIMEOUT = 60;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PushnoyAdaptor adaptor;

    @Inject
    private Account account;

    @Inject
    private ApiClient api;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldDetachDevice() {
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        String authSession = adaptor.getAuthSession(account, deviceUid, sessionId);

        given().conditionEvaluationListener(
                new AllureConditionEvaluationLogger()).pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS).until(() ->
                        JsonPath.parse(adaptor.getDeviceList(USER_TYPE, CLIENT_ID))
                                .read(format("[?(@.device.id == '%s')].device.id", deviceUid), List.class).size(),
                equalTo(1));

        api.auth().logout().reqSpec(defaultSpec()).xSessionIdHeader(authSession).xDeviceUidHeader(deviceUid)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        given().conditionEvaluationListener(
                new AllureConditionEvaluationLogger()).pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS).until(() ->
                        JsonPath.parse(adaptor.getDeviceList(USER_TYPE, CLIENT_ID))
                                .read(format("[?(@.device.id == '%s')].device.id", deviceUid), List.class).size(),
                equalTo(0));
    }

}
