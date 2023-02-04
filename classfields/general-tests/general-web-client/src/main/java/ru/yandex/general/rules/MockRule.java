package ru.yandex.general.rules;

import com.google.inject.Inject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.qameta.allure.Step;
import org.junit.rules.ExternalResource;
import org.mbtest.javabank.Client;
import ru.auto.tests.commons.mountebank.fluent.ImposterBuilder;
import ru.auto.tests.commons.mountebank.http.responses.ModeType;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.general.config.GeneralWebConfig;

import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_OK;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class MockRule extends ExternalResource {

    private String port;

    @Inject
    public GeneralWebConfig config;

    @Inject
    private WebDriverSteps webDriverSteps;

    @Inject
    private ImposterBuilder imposterBuilder;

    @Override
    protected void after() {
        deleteMock();
    }

    /**
     * https://github.com/YandexClassifieds/vertis-mockritsa
     */
    @Step("Создаём мок")
    public void create() {
        HttpResponse<JsonNode> response;
        try {
            response = Unirest.post(config.getMockritsaURL() + "/imposters")
                    .body(imposterBuilder.build().toString()).asJson();
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }

        port = response.getBody().getObject().get("port").toString();
        webDriverSteps.setCookie("mockritsa_imposter", port, config.getBaseDomain());
    }

    public MockRule graphqlStub(String toResponseResource) {
        imposterBuilder.stub().predicate().equals().path("/api/graphql").method(POST).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponseResource)
                .statusCode(HTTP_OK).end().end().end();
        return this;
    }

    public MockRule wizardStub(String toResponseResource) {
        imposterBuilder.stub().predicate().deepEquals().path("/wizard").method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON).body(toResponseResource)
                .statusCode(HTTP_OK).end().end().end();
        return this;
    }

    @Step("Проксируем все запросы с заголовком x-original-host:{from} -> {to}")
    public MockRule transparent(String from, String to) {
        imposterBuilder.stub().predicate().equals().header("x-original-host", from).end().end()
                .response().proxy().to(to).mode(ModeType.PROXY_TRANSPARENT).end().end().end();
        return this;
    }

    public MockRule withDefaults() {
        transparent("general-gateway-api.vrts-slb.test.vertis.yandex.net",
                "http://general-gateway-api.vrts-slb.test.vertis.yandex.net");
        transparent("wizard-01-sas.test.vertis.yandex.net",
                "http://wizard-01-sas.test.vertis.yandex.net");
        return this;
    }

    @Step("Удаляем мок")
    private void deleteMock() {
        Client client = new Client(config.getMockritsaURL());
        Optional.ofNullable(port).ifPresent(port -> client.deleteImposter(Integer.valueOf(port)));
    }

}
