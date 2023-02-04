package ru.auto.tests.publicapi.pushnoy;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.path.json.JsonPath;
import org.awaitility.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.ResponseSpecBuilders;
import ru.auto.tests.publicapi.adaptor.PushnoyAdaptor;
import ru.auto.tests.publicapi.model.AutoApiDevice;
import ru.auto.tests.publicapi.model.AutoApiHelloRequest;
import ru.auto.tests.publicapi.model.VertisPassportLoginOptions;
import ru.auto.tests.publicapi.model.VertisPassportLoginParameters;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("POST /device/hello, POST auth/login")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class AttachDeviceTest {
    private static final String USER_TYPE = "user";
    private static final String TOKEN = "Vertis ios-62ca2575df9c74b3958d118afcbb7602";
    private static final int POLL_INTERVAL = 2;
    private static final int TIMEOUT = 60;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private PushnoyAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldAddDeviceOnLogin() {
        Account account = am.create();
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();

        VertisPassportLoginParameters params = new VertisPassportLoginParameters()
                .login(account.getLogin()).password(account.getPassword()).options(new VertisPassportLoginOptions().allowClientLogin(true));

        String tamper = adaptor.getTamperForRequest(params, deviceUid);

        api.auth().login().body(params)
                .xAuthorizationHeader(TOKEN)
                .xTimestampHeader(tamper)
                .xDeviceUidHeader(deviceUid)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        given().conditionEvaluationListener(
                        new AllureConditionEvaluationLogger()).pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS).until(() ->
                        new JsonPath(adaptor.getDeviceList(USER_TYPE, account.getId())).getString("[0].device.id"),
                equalTo(deviceUid));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldAddDeviceOnHello() {
        Account account = am.create();
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        String authSession = adaptor.getAuthSession(account, deviceUid, sessionId);
        adaptor.deleteDeviceFromUser(USER_TYPE, account.getId(), deviceUid);
        AutoApiHelloRequest request = new AutoApiHelloRequest().device(new AutoApiDevice());
        String tamper = adaptor.getTamperForRequest(request, deviceUid);

        api.device().hello().body(request)
                .reqSpec(defaultSpec()).xSessionIdHeader(authSession)
                .xDeviceUidHeader(deviceUid)
                .xAuthorizationHeader(TOKEN)
                .xTimestampHeader(tamper)
                .execute(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));

        given().conditionEvaluationListener(
                        new AllureConditionEvaluationLogger()).pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS).until(() ->
                        new JsonPath(adaptor.getDeviceList(USER_TYPE, account.getId())).getString("[0].device.id"),
                equalTo(deviceUid));
    }

}
