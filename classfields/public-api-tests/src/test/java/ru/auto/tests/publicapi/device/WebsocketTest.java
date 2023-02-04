package ru.auto.tests.publicapi.device;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.model.AutoApiSecretSignResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


/**
 * Created by vicdev on 15.09.17.
 */

@DisplayName("GET /device/websocket")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class WebsocketTest {

    private static final String PUSH_SANDBOX_HOST = "push-sandbox.yandex.ru";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;


    @Test
    public void shouldSee403WhenNoAuth() {
        api.device().websocket().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSeeHost() {
        AutoApiSecretSignResponse response = api.device().websocket().reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON()))
                .as(AutoApiSecretSignResponse.class);
        AutoruApiModelsAssertions.assertThat(response).hasHost(PUSH_SANDBOX_HOST);
    }

    @Test
    public void shouldSeeUrl() {
        Response response = api.device().websocket().reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON()));
        AutoApiSecretSignResponse websocketResponse = response.as(AutoApiSecretSignResponse.class);
        String deviceUid = response.getHeader("X-Device-UID");
        AutoruApiModelsAssertions.assertThat(websocketResponse).hasUrl(String.format("wss://%s/v2/subscribe/websocket?sign=%s&ts=%s" +
                        "&service=autoru&user=%s&client=swagger&session=session", PUSH_SANDBOX_HOST,
                websocketResponse.getSign(), websocketResponse.getTs(), deviceUid));
    }


    @Test
    public void shouldTsChange() {
        AutoApiSecretSignResponse response = api.device().websocket().reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON()))
                .as(AutoApiSecretSignResponse.class);
        AutoApiSecretSignResponse anotherResponse = api.device().websocket().reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON()))
                .as(AutoApiSecretSignResponse.class);
        assertThat("Поле <ts> не изменилось при повторном запросе", response.getTs(),
                not(equalTo(anotherResponse.getSign())));

    }

    @Test
    public void shouldSignChange() {
        AutoApiSecretSignResponse response = api.device().websocket().reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON()))
                .as(AutoApiSecretSignResponse.class);
        AutoApiSecretSignResponse anotherResponse = api.device().websocket().reqSpec(defaultSpec()).execute(validatedWith(shouldBe200OkJSON()))
                .as(AutoApiSecretSignResponse.class);
        assertThat("Поле <sign> не изменилось при повторном запросе", response.getTs(), not(equalTo(anotherResponse.getSign())));

    }
}
