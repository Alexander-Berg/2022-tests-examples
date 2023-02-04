package ru.yandex.qe.dispenser.ws.api;

import javax.ws.rs.HttpMethod;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiPersonInfo;


public final class PersonServiceApiTest extends ApiTestBase {
    @Test
    public void getPersonInfoRegressionTest() {
        final DiPersonInfo personInfo = dispenser().persons().getInfoFor("whistler").perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/persons/info?login=whistler");
        assertLastResponseEquals("/body/person/info/whistler.json");
        assertJsonEquals("/body/person/info/whistler.json", personInfo);
    }
}
