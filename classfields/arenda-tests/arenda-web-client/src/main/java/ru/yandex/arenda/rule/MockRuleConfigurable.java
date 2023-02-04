package ru.yandex.arenda.rule;

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
import ru.yandex.arenda.config.ArendaWebConfig;

import java.util.Optional;

import static java.lang.Integer.parseInt;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockRuleConfigurable extends ExternalResource {

    private static final String COOKIE_DOMAIN = ".yandex.ru";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final int OK_200 = 200;

    private String port;

    @Inject
    public ArendaWebConfig config;

    @Inject
    public WebDriverSteps webDriverSteps;

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
        create(imposterBuilder.build().toString());
    }

    @Step("Создаём мок")
    public void create(String imposter) {
        HttpResponse<JsonNode> response;

        try {
            response = Unirest.post(config.getMockritsaURL() + "/imposters")
                    .body(imposter).asJson();
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }

        port = response.getBody().getObject().get("port").toString();
        webDriverSteps.setCookie("mockritsa_imposter", port, COOKIE_DOMAIN);
    }

    public void createWithDefaults() {
        withDefaults();
        create();
    }

    @Step("Добавляем мок для «/2.0/rent/moderation/flats/search/by-address»")
    public MockRuleConfigurable byAddressStub(String toResponse) {
        imposterBuilder.stub().predicate().equals().path("/2.0/rent/moderation/flats/search/by-address")
                .method(GET).end().end()
                .response().is().header(CONTENT_TYPE, APPLICATION_JSON)
                .body(toResponse)
                .statusCode(OK_200).end().end().end();
        return this;
    }

    @Step("Проксируем все запросы с заголовком x-original-host:{from} -> {to}")
    public MockRuleConfigurable transparent(String from, String to) {
        imposterBuilder.stub().predicate().equals().header("x-original-host", from).end().end()
                .response().proxy().to(to).mode(ModeType.PROXY_TRANSPARENT).end().end().end();
        return this;
    }

    public MockRuleConfigurable withDefaults() {
        transparent("realty-gateway-api.vrts-slb.test.vertis.yandex.net", "http://realty-gateway-api.vrts-slb.test.vertis.yandex.net");
        return this;
    }

    public static String forResponse(String response) {
        return getResourceAsString(response);
    }

    @Step("Удаляем мок")
    private void deleteMock() {
        Client client = new Client(config.getMockritsaURL());
        Optional.ofNullable(port).ifPresent(port -> client.deleteImposter(parseInt(port)));
    }
}