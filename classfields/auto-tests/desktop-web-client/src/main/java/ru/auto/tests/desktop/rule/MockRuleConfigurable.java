package ru.auto.tests.desktop.rule;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import org.junit.rules.ExternalResource;
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.mock.MockStub;
import ru.auto.tests.desktop.step.CookieSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.apache.http.HttpStatus.SC_CREATED;
import static ru.auto.tests.desktop.utils.Utils.getJsonByPath;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;

public class MockRuleConfigurable extends ExternalResource {

    private final static String IMPOSTERS = "imposters";
    private final static String STUBS = "stubs";

    @Inject
    public DesktopConfig config;

    @Inject
    public CookieSteps cookieSteps;

    @Getter
    private String port;

    @Getter
    @Setter
    private JsonObject json;

    @Override
    protected void after() {
        delete();
    }

    public MockRuleConfigurable() {
        json = getJsonByPath("mocks/desktop/Default.json");
    }

    public MockRuleConfigurable setStubs(MockStub... stubs) {
        stream(stubs).forEach(stub -> json.getAsJsonArray(STUBS).add(getJsonObject(stub.getStub())));
        return this;
    }

    @Step("Публикуем мок")
    public void create() {
        HttpResponse<JsonNode> response;
        String mockritsaImpostersUrl = format("%s/%s", config.getMockritsaApiUrl(), IMPOSTERS);

        try {
            response = Unirest.post(mockritsaImpostersUrl)
                    .body(json.toString())
                    .asJson();

            if (response.getStatus() == SC_CREATED) {
                port = response.getBody().getObject().get("port").toString();
            } else {
                throw new RuntimeException(format("Mokritsa response code: %s", response.getStatus()));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        cookieSteps.setMockritsaPortAndHostCookies(port);
    }

    @Step("Обновляем существующий мок")
    public void update() {
        String mockritsaImpostersPortStubsUrl = format("%s/%s", getMockritsaImpostersPortUrl(), STUBS);

        try {
            JsonObject stubsRequest = new JsonObject();
            stubsRequest.add(STUBS, json.getAsJsonArray(STUBS));

            Unirest.put(mockritsaImpostersPortStubsUrl)
                    .body(stubsRequest.toString())
                    .asJson();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Меняем stub[{index}]")
    public void overwriteStub(int index, MockStub stub) {
        String mockritsaImpostersPortStubsIndexUrl = format("%s/%s/%d", getMockritsaImpostersPortUrl(), STUBS, index);

        try {
            Unirest.put(mockritsaImpostersPortStubsIndexUrl)
                    .body(getJsonObject(stub.getStub()).toString())
                    .asString();

        } catch (Exception e) {
            throw new RuntimeException("Не удалось перезаписать мок", e.getCause());
        }

        json.getAsJsonArray(STUBS).set(index, getJsonObject(stub.getStub()));
    }

    @Step("Удаляем мок")
    public void delete() {
        try {
            Unirest.delete(getMockritsaImpostersPortUrl()).asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        JsonArray stubs = new JsonArray();
        json.add(STUBS, stubs);
    }

    private String getMockritsaImpostersPortUrl() {
        return format("%s/%s/%s", config.getMockritsaApiUrl(), IMPOSTERS, port);
    }

}
