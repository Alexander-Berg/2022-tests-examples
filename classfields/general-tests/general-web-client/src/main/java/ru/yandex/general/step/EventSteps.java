package ru.yandex.general.step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import org.awaitility.core.ThrowingRunnable;
import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;
import org.json.simple.JSONArray;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.browsermob.ProxyServerManager;
import ru.yandex.general.beans.events.Event;
import ru.yandex.general.beans.events.Events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public class EventSteps {

    private static final String EVENT_INFO = "eventInfo";
    private static final String QUERY_ID = "queryId";

    private String textParams;

    private List<ThrowingRunnable> assertions = new ArrayList<>();


    private String[] pathsToBeIgnored = {};

    private String eventType;

    private List<JsonObject> actualEvents;

    private Matcher<String> matcherUrl = StringContains.containsString("sendEvents");

    @Inject
    @Getter
    private ProxyServerManager proxyServerManager;

    @Step("Единственное событие с параметрами «{eventParams}»")
    public EventSteps singleEventWithParams(Event eventParams) {
        this.textParams = new Gson().toJson(eventParams);
        assertions.add(() -> {
            List<JsonObject> filteredEvents;
            filteredEvents = actualEvents.stream()
                    .filter(event -> event.getAsJsonObject("context").get("block").getAsString()
                            .equals(eventParams.getContext().getBlock()))
                    .collect(Collectors.toList());
            assertThat(format("Единственное событие типа «%s»", eventType), filteredEvents, hasSize(1));
            assertThatJson(filteredEvents.get(0)).whenIgnoringPaths(pathsToBeIgnored)
                    .isEqualTo(textParams);
        });
        return this;
    }

    @Step("С событими в количестве «{count}»")
    public EventSteps withEventsCount(int count) {
        assertions.add(() -> {
            assertThat(format("Количество событий «%d»", count), actualEvents, hasSize(count));
        });
        return this;
    }

    @Step("С событими в количестве «{matcherCount}»")
    public EventSteps withEventsCount(Matcher<Integer> matcherCount) {
        assertions.add(() -> {
            assertThat(format("Количество событий «%s»", matcherCount), actualEvents, hasSize(matcherCount));
        });
        return this;
    }

    @Step("Фильтруем события по queryId")
    public EventSteps queryIdMatcher(Matcher<String> matcherQueryId) {
        assertions.add(() -> {
            int actualEventsSize = actualEvents.size();
            List<JsonObject> eventsMatchQueryId = actualEvents.stream()
                    .filter(event -> matcherQueryId.matches(event.get(QUERY_ID).getAsString()))
                    .collect(Collectors.toList());
            assertThat(format("QueryId соответствует в «%d» событиях", actualEventsSize),
                    eventsMatchQueryId, hasSize(actualEventsSize));
        });
        return this;
    }

    @Step("Cобытия с полем page = «{pageNumber}»")
    public EventSteps allEventsWithPageNumber(int pageNumber) {
        assertions.add(() -> {
            assertThat(format("Кол-во событий типа «%s» больше 0", eventType), actualEvents, hasSize(greaterThan(0)));

            List<JsonObject> eventsWithPageNumber = actualEvents.stream().filter(event ->
                    event.getAsJsonObject(EVENT_INFO).getAsJsonObject(eventType).get("page").getAsInt() == pageNumber)
                    .collect(Collectors.toList());

            assertThat(format("Все события типа «%s» с page = «{pageNumber}»", eventType),
                    actualEvents.size(), equalTo(eventsWithPageNumber.size()));
        });
        return this;
    }

    @Step("Проверяем что индексы события «snippetShow» уникальны и в верном диапазоне")
    public EventSteps snippetShowIndexesUniqAndInRange(int eventsCount) {
        assertions.add(() -> {
            int uniqIndexesInRangeCount = (int) actualEvents.stream()
                    .map(event -> event.getAsJsonObject(EVENT_INFO).getAsJsonObject("snippetShow")
                            .get("index").getAsInt())
                    .collect(Collectors.toList())
                    .stream()
                    .filter(index -> index > 0)
                    .filter(index -> index <= actualEvents.size() + 1)
                    .distinct()
                    .count();

            assertThat("Индексы событий «snippetShow» уникальны и в верном диапазоне",
                    uniqIndexesInRangeCount, equalTo(eventsCount));
        });
        return this;
    }

    @Step("С событиями «{eventType}»")
    public EventSteps withEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public EventSteps withIgnoringPaths(String... paths) {
        pathsToBeIgnored = paths;
        return this;
    }

    @Step("Ждем соответствующего события")
    public void shouldExist() {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(3, SECONDS)
                .pollInterval(1, SECONDS)
                .atMost(8, SECONDS)
                .untilAsserted(() ->
                {
                    actualEvents = getEventEntries();
                    AtomicBoolean asserted = new AtomicBoolean(true);
                    AtomicReference<String> message = new AtomicReference<>("");
                    assertions.forEach(assertion -> {
                        try {
                            assertion.run();
                        } catch (AssertionError error) {
                            asserted.set(false);
                            message.set(error.getMessage());
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
                    if (!asserted.get()) throw new AssertionError(message.get());
                });
    }

    @Step("Получаем queryId")
    public String getQueryId() {
        return actualEvents.get(0).get(QUERY_ID).getAsString();
    }

    public List<JsonObject> getEventEntries() {
        List<Event> allEvents = new JSONArray();

        proxyServerManager.getServer().getHar().getLog().getEntries().stream()
                .filter(harEntry -> matcherUrl.matches(harEntry.getRequest().getUrl()))
                .filter(harEntry -> equalTo(harEntry.getResponse().getStatus()).matches(200))
                .forEach(harEntry -> {
                    Events events = new GsonBuilder().create().fromJson(
                            harEntry.getRequest().getPostData().getText(), Events.class);
                    allEvents.addAll(events.getEvents());
                });

        return allEvents.stream()
                .map(event -> new Gson().toJsonTree(event).getAsJsonObject())
                .filter(event -> {
                    if (eventType != null) return event.getAsJsonObject(EVENT_INFO).has(eventType);
                    else return true;
                })
                .collect(Collectors.toList());
    }

    @Step("Очищаем HAR диаграмму")
    public EventSteps clearHar() {
        proxyServerManager.getServer().newHar();
        assertions.clear();
        eventType = null;
        return this;
    }

}
