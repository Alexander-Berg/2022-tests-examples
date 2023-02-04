package ru.auto.tests.desktop.step;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import io.qameta.allure.Step;
import ru.auto.tests.desktop.DesktopConfig;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;

public class BunkerSteps {

    @Inject
    public DesktopConfig config;

    @Inject
    public UrlSteps urlSteps;

    @Step("Получаем узел {node} из бункера")
    public JsonNode getBunkerNode(String node) {
        try {
            HttpResponse<JsonNode> response = Unirest.get(urlSteps.fromUri(config.getBunkerApiUrl()).path("/v1/cat")
                    .addParam("node", node).toString()).asJson();
            if (response.getStatus() == SC_OK) {
                return response.getBody();
            } else {
                throw new RuntimeException(format("Bunker response code: %s", response.getStatus()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}