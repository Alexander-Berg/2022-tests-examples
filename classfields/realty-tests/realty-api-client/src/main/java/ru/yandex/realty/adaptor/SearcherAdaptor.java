package ru.yandex.realty.adaptor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.restassured.mapper.ObjectMapperType;
import ru.auto.test.api.realty.ApiSearcher;
import ru.auto.test.api.realty.card.json.responses.Card;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;

import java.util.List;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;

/**
 * Created by vicdev on 13.04.17.
 */
public class SearcherAdaptor extends AbstractModule {

    private static final int POLL_DELAY = 5;
    private static final int TIMEOUT = 51;

    @Inject
    private ApiSearcher apiSearcher;

    @Step("Ждем офферы в серчере с ids={ids}")
    public void waitOffers(List<String> ids) {
        ids.forEach(this::waitOffer);
    }

    @Step("Ждем оффер в серчере с id={id} со статусом «active»")
    public void waitOffer(String id) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await()
                .pollInterval(POLL_DELAY, SECONDS).atMost(TIMEOUT, SECONDS)
                .alias("Ждали что статус оффера будет «active»")
                .until(() -> equalTo(SC_OK).matches(
                        apiSearcher.cardjson().withId(id).get(Function.identity()).andReturn().statusCode()) &&
                        apiSearcher.cardjson().withId(id).get(validatedWith(shouldBe200Ok()))
                                .as(Card.class, ObjectMapperType.GSON).getData().get(0).getOffers().get(0)
                                .getActive());
    }

    @Step("Ждем появления оффера id={id} в серчере")
    public void waitSearcher(String id) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await()
                .pollInterval(POLL_DELAY, SECONDS).atMost(TIMEOUT, SECONDS)
                .until(() -> apiSearcher.cardjson().withId(id).get(Function.identity()).andReturn().statusCode(),
                        equalTo(SC_OK));
    }

    @Override
    protected void configure() {

    }
}
