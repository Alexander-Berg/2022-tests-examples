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
import ru.auto.tests.publicapi.ResponseSpecBuilders;
import ru.auto.tests.publicapi.adaptor.PushnoyAdaptor;
import ru.auto.tests.publicapi.model.AutoApiDevice;
import ru.auto.tests.publicapi.model.AutoApiHelloRequest;
import ru.auto.tests.publicapi.model.VertisPassportLoginOptions;
import ru.auto.tests.publicapi.model.VertisPassportLoginParameters;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;

import java.util.List;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.DealerConsts.CLIENT_ID;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("POST /device/hello, POST auth/login")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiDealerModule.class)
public class DealerAttachDeviceTest {
    private static final String DEALER = "dealer";
    private static final String USER = "user";
    private static final String TOKEN = "Vertis ios-62ca2575df9c74b3958d118afcbb7602";
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
    public void shouldAttachDeviceToDealerOnLogin() {
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();

        VertisPassportLoginParameters params = new VertisPassportLoginParameters()
                .login(account.getLogin()).password(account.getPassword()).options(new VertisPassportLoginOptions().allowClientLogin(true));

        String tamper = adaptor.getTamperForRequest(params, deviceUid);

        api.auth().login().body(params)
                .xAuthorizationHeader(TOKEN)
                .xDeviceUidHeader(deviceUid)
                .xSessionIdHeader(sessionId)
                .xTimestampHeader(tamper)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        given().conditionEvaluationListener(
                        new AllureConditionEvaluationLogger()).pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS).until(() ->
                                JsonPath.parse(adaptor.getDeviceList(DEALER, CLIENT_ID))
                                .read(format("[?(@.device.id == '%s')].device.id", deviceUid), List.class).size(),
                equalTo(1));

        String actualDeviceUid = JsonPath.parse(adaptor.getDeviceList(DEALER, CLIENT_ID))
                .read(format("[?(@.device.id == '%s')].device.id", deviceUid), List.class).get(0).toString();

        assertThat(actualDeviceUid).isEqualTo(deviceUid);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldAttachDeviceToDealerOnHello() {
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        String authSession = adaptor.getAuthSession(account, deviceUid, sessionId);
        adaptor.deleteDeviceFromUser(DEALER, CLIENT_ID, deviceUid);
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
                                JsonPath.parse(adaptor.getDeviceList(DEALER, CLIENT_ID))
                                .read(format("[?(@.device.id == '%s')].device.id", deviceUid), List.class).size(),
                equalTo(1));

        String actualDeviceUid = JsonPath.parse(adaptor.getDeviceList(DEALER, CLIENT_ID))
                .read(format("[?(@.device.id == '%s')].device.id", deviceUid), List.class).get(0).toString();

        assertThat(actualDeviceUid).isEqualTo(deviceUid);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldNotAttachDeviceToUserOnLogin() {
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();

        VertisPassportLoginParameters params = new VertisPassportLoginParameters()
                .login(account.getLogin()).password(account.getPassword()).options(new VertisPassportLoginOptions().allowClientLogin(true));

        String tamper = adaptor.getTamperForRequest(params, deviceUid);

        api.auth().login().body(params)
                .xAuthorizationHeader(TOKEN)
                .xDeviceUidHeader(deviceUid)
                .xSessionIdHeader(sessionId)
                .xTimestampHeader(tamper)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        List<String> devices = JsonPath.parse(adaptor.getDeviceList(USER, account.getId())).read(format("[?(@.device.id == '%s')].device.id", deviceUid));
        assertThat(devices).isEmpty();
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldNotAttachDeviceToUserOnHello() {
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        String authSession = adaptor.getAuthSession(account, deviceUid, sessionId);
        adaptor.deleteDeviceFromUser(USER, account.getId(), deviceUid);
        AutoApiHelloRequest request = new AutoApiHelloRequest().device(new AutoApiDevice());

        String tamper = adaptor.getTamperForRequest(request, deviceUid);

        api.device().hello().body(request)
                .reqSpec(defaultSpec()).xSessionIdHeader(authSession)
                .xDeviceUidHeader(deviceUid)
                .xAuthorizationHeader(TOKEN)
                .xTimestampHeader(tamper)
                .execute(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));

        List<String> devices = JsonPath.parse(adaptor.getDeviceList(USER, account.getId())).read(format("[?(@.device.id == '%s')].device.id", deviceUid));
        assertThat(devices).isEmpty();
    }
}
