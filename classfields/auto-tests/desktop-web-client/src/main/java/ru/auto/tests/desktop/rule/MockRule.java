package ru.auto.tests.desktop.rule;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import org.json.simple.parser.ParseException;
import org.junit.rules.ExternalResource;
import org.mbtest.javabank.Client;
import org.mbtest.javabank.http.imposters.Imposter;
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.step.CookieSteps;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

@Deprecated
public class MockRule extends ExternalResource {

    private final static String MOCKRITSA_HOST_COOKIE = "mockritsa_mock_host";
    private final static String MOCKRITSA_PORT_COOKIE = "mockritsa_imposter";
    private final static Map<String, String> REPLACE_PARAMS = new HashMap<>();

    @Inject
    public DesktopConfig config;

    @Inject
    public CookieSteps cookieSteps;

    @Getter
    private String port;

    @Getter
    @Setter
    private JsonObject json;

    @Deprecated
    public MockRule replaceJsonParam(String key, String value) {
        REPLACE_PARAMS.put(key, value);
        return this;
    }

    @Override
    protected void after() {
        delete();
    }

    @Deprecated
    @Step("Создаём мок на основе {jsonFilename}.json")
    public void createMock(String jsonFilename) {
        String jsonText = getResourceAsString(format("mocks/%s.json", jsonFilename));

        for (Map.Entry<String, String> entry : REPLACE_PARAMS.entrySet()) {
            jsonText = jsonText.replaceAll(entry.getKey(), entry.getValue());
        }

        HttpResponse<JsonNode> response;

        try {
            response = Unirest.post(config.getMockritsaApiUrl() + "/imposters").body(jsonText).asJson();
            port = response.getBody().getObject().get("port").toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        setCookies();
    }

    @Deprecated
    @Step("Создаём мок")
    public MockRule newMock() {
        json = new Gson().fromJson(getResourceAsString("mocks/desktop/Default.json"), JsonObject.class);
        return this;
    }

    @Deprecated
    @Step("Создаём мок на основе {jsonFilename}.json")
    public MockRule newMock(String jsonFilename) {
        json = new Gson().fromJson(getResourceAsString(jsonFilename), JsonObject.class);
        return this;
    }

    @Deprecated
    @Step("Добавляем ручки")
    public MockRule with(String... jsonFiles) {
        if (json == null) {
            throw new RuntimeException("JSON object is Null. Forget about .newMock() call?");
        }

        for (String jsonFile : jsonFiles) {
            try {
                json.getAsJsonArray("stubs").add(new Gson()
                        .fromJson(getResourceAsString(format("mocks/%s.json", jsonFile)), JsonObject.class));
            } catch (NullPointerException e) {
                String exceptionMessage = format("Can't read mock file '%s'", jsonFile);
                throw new RuntimeException(exceptionMessage, e.getCause());
            }
        }

        return this;
    }

    @Deprecated
    @Step("Добавляем ручки")
    public MockRule with(JsonObject... jsons) {
        for (JsonObject jsonObject : jsons) {
            json.getAsJsonArray("stubs").add(jsonObject);
        }

        return this;
    }

    @Deprecated
    @Step("Заменяем параметр {param} на {value}")
    public MockRule replaceParam(String param, String value) {
        json = new Gson().fromJson(json.toString().replace(param, value), JsonObject.class);

        return this;
    }

    @Deprecated
    @Step("Публикуем мок")
    public void post() {
        HttpResponse<JsonNode> response;

        try {
            response = Unirest.post(config.getMockritsaApiUrl() + "/imposters")
                    .body(json.toString()).asJson();
            if (response.getStatus() == SC_CREATED) {
                port = response.getBody().getObject().get("port").toString();
            } else {
                throw new RuntimeException(format("Mokritsa response code: %s", response.getStatus()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        setCookies();
    }

    @Deprecated
    @Step("Обновляем существующий мок")
    public void update() {
        try {
            Unirest.put(config.getMockritsaApiUrl() + "/imposters/" + port + "/stubs")
                    .body("{\"stubs\":" + json.getAsJsonArray("stubs").toString() + "}").asJson();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    @Step("Меняем stub[{index}] на {jsonFile}")
    public void overwriteStub(int index, String jsonFile) {
        try {
            Unirest.put(config.getMockritsaApiUrl() + "/imposters/" + port + "/stubs/" + index)
                    .body(getResourceAsString(format("mocks/%s.json", jsonFile))).asString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        json.getAsJsonArray("stubs").set(index, new Gson()
                .fromJson(getResourceAsString(format("mocks/%s.json", jsonFile)), JsonObject.class));
    }

    @Deprecated
    @Step("Устанавливаем actualize_date = {actualizeDate}")
    public JsonObject setActualizeDate(String jsonFile, String actualizeDate) {
        JsonObject json = new Gson().fromJson(getResourceAsString(format("mocks/%s.json", jsonFile)), JsonObject.class);
        json.getAsJsonArray("responses")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("is")
                .getAsJsonObject("body")
                .getAsJsonObject("offer")
                .getAsJsonObject("additional_info")
                .addProperty("actualize_date", actualizeDate);
        return json;
    }

    @Deprecated
    @Step("Устанавливаем expire_date = {dateValue} для оффера в ручке /offer")
    public JsonObject setOfferExpireDate(String jsonFile, String dateValue) {
        JsonObject json = new Gson().fromJson(getResourceAsString(format("mocks/%s.json", jsonFile)), JsonObject.class);
        json.getAsJsonArray("responses")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("is")
                .getAsJsonObject("body")
                .getAsJsonObject("offer")
                .getAsJsonObject("additional_info")
                .addProperty("expire_date", dateValue);
        return json;
    }

    @Deprecated
    @Step("Устанавливаем expire_date = {dateValue} для оффера в ручке /user/offers")
    public JsonObject setUserOffersExpireDate(String jsonFile, String dateValue) {
        JsonObject json = new Gson().fromJson(getResourceAsString(format("mocks/%s.json", jsonFile)), JsonObject.class);
        json.getAsJsonArray("responses")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("is")
                .getAsJsonObject("body")
                .getAsJsonArray("offers")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("additional_info")
                .addProperty("expire_date", dateValue);
        return json;
    }

    @Deprecated
    public MockRule setRecordRequests(Boolean recordRequests) {
        json.addProperty("recordRequests", recordRequests);
        return this;
    }

    @Deprecated
    @Step("Получаем мок")
    public Imposter getMock() {
        Client client = new Client(config.getMockritsaApiUrl());
        Imposter imposter;
        try {
            imposter = client.getImposter(Integer.parseInt(port));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return imposter;
    }

    @Deprecated
    @Step("Удаляем мок")
    public void delete() {
        try {
            HttpResponse<JsonNode> response = Unirest.delete(config.getMockritsaApiUrl() + "/imposters/" + port)
                    .asJson();
            if (response.getStatus() != SC_OK) {
                Unirest.delete(config.getMockritsaApiUrl() + "/imposters/" + port);
            }
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    @Step("Выставляем куки мокрицы")
    public void setCookies() {
        cookieSteps.setCookie(MOCKRITSA_HOST_COOKIE, config.getMockritsaMockHost(),
                format(".%s", config.getBaseDomain()));
        cookieSteps.setCookie(MOCKRITSA_PORT_COOKIE, port, format(".%s", config.getBaseDomain()));
    }

    @Deprecated
    @Step("Удаляем куки мокрицы")
    public void deleteCookies() {
        cookieSteps.deleteCookie(MOCKRITSA_HOST_COOKIE);
        cookieSteps.deleteCookie(MOCKRITSA_PORT_COOKIE);
    }
}