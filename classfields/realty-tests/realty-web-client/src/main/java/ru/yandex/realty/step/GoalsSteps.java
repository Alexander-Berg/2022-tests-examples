package ru.yandex.realty.step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import net.lightbody.bmp.core.har.HarEntry;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.browsermob.ProxyServerManager;
import ru.yandex.realty.beans.Goal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class GoalsSteps {

    public static final String EVENT_GATE_STAT_CARD = "gate/stat/card/";
    public static final String EVENT_GATE_STAT_OFFER = "gate/stat/offer/";
    public static final String EVENT_GATE_STAT_RENEWAL = "gate/stat/renewal/";
    private static final String EVENT_RESPONSE_VALID_TRUE = "{\"response\":[\"valid\",[true]]}";
    public static final String RESPONSE_STATUS_OK = "{\"response\":{\"status\":\"OK\"}}";

    private String textParams;

    private Matcher matcherGoal;

    private Supplier<JsonObject> actual;

    private String[] pathsToBeIgnored = {};

    @Inject
    private ProxyServerManager proxyServerManager;

    @Step("Проверяем что уходит запрос с целью {matcherGoal}")
    public GoalsSteps urlMatcher(Matcher matcherGoal) {
        this.matcherGoal = matcherGoal;
        return this;
    }

    @Step("C параметрами {goalParams}")
    public GoalsSteps withGoalParams(Goal goalParams) {
        this.textParams = new Gson().toJson(goalParams);
        actual = () -> getActualGoalParams();
        return this;
    }

    private JsonObject getActualGoalParams() {
        String mimeType = getGoalEntries().get()
                .findFirst().get().getRequest().getPostData().getMimeType();
        String text;
        //в зависимости от типа mimeType параметры могут падать в params или в text
        switch (mimeType) {
            case "text/plain;charset=UTF-8":
                text = getGoalEntries().get()
                        .findFirst().get().getRequest().getPostData().getText()
                        .substring("site-info=".length());
                try {
                    text = URLDecoder.decode(text, UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                text = getGoalEntries().get()
                        .findFirst().get().getRequest().getPostData().getParams().stream()
                        .filter(h -> equalTo("site-info").matches(h.getName())).findFirst().get().getValue();
                break;
        }

        return new GsonBuilder().create().fromJson(text, JsonObject.class);
    }

    private Supplier<Stream<HarEntry>> getGoalEntries() {
        return () -> proxyServerManager.getServer().getHar().getLog().getEntries().stream()
                .filter(e -> matcherGoal.matches(e.getRequest().getUrl()))
                .filter(e -> equalTo(e.getResponse().getStatus()).matches(HttpStatus.SC_OK));
    }

    @Step("C параметрами события {pathToTemplate}")
    public GoalsSteps withEventParams(String pathToTemplate) {
        this.textParams = getResourceAsString(pathToTemplate);
        actual = () -> getActualEventParams();
        return this;
    }

    public GoalsSteps withIgnoringPaths(String... paths) {
        pathsToBeIgnored = paths;
        return this;
    }

    private JsonObject getActualEventParams() {
        String text = proxyServerManager.getServer().getHar().getLog().getEntries().stream()
                .filter(e -> matcherGoal.matches(e.getRequest().getUrl()))
                .filter(e -> equalTo(e.getResponse().getStatus()).matches(HttpStatus.SC_OK))
//включить когда будет работать
//                .filter(e -> equalTo(e.getResponse().getContent().getText()).matches(EVENT_RESPONSE_VALID_TRUE))

                .findFirst().get().getRequest().getPostData().getText();
        return new GsonBuilder().create().fromJson(text, JsonObject.class);
    }

    private JsonObject getActualParamsWithResponse(String response) {
        String text = proxyServerManager.getServer().getHar().getLog().getEntries().stream()
                .filter(e -> matcherGoal.matches(e.getRequest().getUrl()))
                .filter(e -> equalTo(e.getResponse().getStatus()).matches(HttpStatus.SC_OK))
                .filter(e -> equalTo(e.getResponse().getContent().getText()).matches(response))
                .findFirst().get().getRequest().getPostData().getText();
        return new GsonBuilder().create().fromJson(text, JsonObject.class);
    }

    @Step("C параметрами")
    public GoalsSteps withParams(String params) {
        this.textParams = params;
        actual = () -> getActualEventParams();
        return this;
    }

    @Step("C параметрами и ответом")
    public GoalsSteps withParamsAndResponse(String params, String response) {
        this.textParams = params;
        actual = () -> getActualParamsWithResponse(response);
        return this;
    }

    @Step("Ждем...")
    public void shouldExist() {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).pollInSameThread().ignoreExceptions()
                .pollInterval(1, SECONDS)
                .atMost(6, SECONDS)
                .alias(format("Ожидали что запрос с  %s ушёл", matcherGoal))
                .untilAsserted(() ->
                        assertThatJson(actual.get()).whenIgnoringPaths(pathsToBeIgnored).isEqualTo(textParams));
    }
}
