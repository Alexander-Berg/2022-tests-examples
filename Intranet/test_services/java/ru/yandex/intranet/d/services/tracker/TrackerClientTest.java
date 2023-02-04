package ru.yandex.intranet.d.services.tracker;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.http.HttpStatus;

import ru.yandex.intranet.d.util.result.ErrorCollection;
import ru.yandex.intranet.d.util.result.Result;
import ru.yandex.intranet.d.util.result.TypedError;
import ru.yandex.intranet.d.web.model.tracker.TrackerCreateTicketDto;
import ru.yandex.intranet.d.web.model.tracker.TrackerCreateTicketResponseDto;
import ru.yandex.intranet.d.web.model.tracker.TrackerErrorDto;

/**
 * Tests for tracker client
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@ExtendWith(MockServerExtension.class)
public class TrackerClientTest {

    private static final String TOKEN = "testToken";
    private static final String QUEUE = "DISPENSER_TEST_QUEUE";

    @Test
    public void testCreateTicket(MockServerClient trackerServerMock) throws JsonProcessingException {
        String key = QUEUE + "-1";
        TrackerCreateTicketResponseDto responseMock = new TrackerCreateTicketResponseDto(key);
        trackerServerMock.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath("/v2/issues")
                        .withHeader("Authorization", "OAuth " + TOKEN)
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(HttpStatus.OK.value())
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(TrackerCreateTicketResponseDto.class)
                        .writeValueAsString(responseMock))
        );

        String host = trackerServerMock.remoteAddress().getHostString();
        int port = trackerServerMock.remoteAddress().getPort();
        TrackerClient client = new TrackerClientImpl(5000, 5000, 5000, 2, 1000,
                "http://" + host + ":" + port, TOKEN, "d");
        TrackerCreateTicketDto body = new TrackerCreateTicketDto(QUEUE, "Test summary",
                "desc", "username", List.of(45989L), List.of(), UUID.randomUUID().toString());

        Result<TrackerCreateTicketResponseDto> result = client.createTicket(body).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(key, result.match(TrackerCreateTicketResponseDto::getKey, error -> null));
        trackerServerMock.reset();
    }

    @Test
    public void testGenNewRequestIdForRetry(MockServerClient trackerServerMock) throws JsonProcessingException {
        TrackerCreateTicketResponseDto responseMock = new TrackerCreateTicketResponseDto(QUEUE + "-1");
        HttpRequest request = HttpRequest.request()
                .withMethod("POST")
                .withPath("/v2/issues")
                .withHeader("Authorization", "OAuth " + TOKEN)
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString());
        trackerServerMock.when(request, Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(HttpStatus.BAD_GATEWAY.value())
        );
        trackerServerMock.when(request, Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(HttpStatus.OK.value())
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(TrackerCreateTicketResponseDto.class)
                        .writeValueAsString(responseMock))
        );

        String host = trackerServerMock.remoteAddress().getHostString();
        int port = trackerServerMock.remoteAddress().getPort();
        TrackerClient client = new TrackerClientImpl(5000, 5000, 5000, 2, 1000,
                "http://" + host + ":" + port, TOKEN, "d");
        TrackerCreateTicketDto body = new TrackerCreateTicketDto(QUEUE, "Test summary",
                "desc", "username", List.of(45989L), List.of(), UUID.randomUUID().toString());

        client.createTicket(body).block();
        String logType = "received request:";
        Set<String> requestIds = Arrays.stream(trackerServerMock.retrieveLogMessagesArray(request))
                .filter(log -> log.contains(logType))
                .map(log -> log.substring(log.indexOf(logType) + logType.length()))
                .map(json -> {
                    try {
                        return new ObjectMapper().registerModule(new Jdk8Module())
                                .readTree(json).get("headers").get("X-Request-ID").get(0).textValue();
                    } catch (NullPointerException | JsonProcessingException e) {
                        Assertions.fail(e);
                        return null;
                    }
                })
                .collect(Collectors.toSet());

        Assertions.assertEquals(2, requestIds.size());
        trackerServerMock.reset();
    }

    @Test
    public void testCreateTicketWithConflict(MockServerClient trackerServerMock) {
        String key = QUEUE + "-1";
        trackerServerMock.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath("/v2/issues")
                        .withHeader("Authorization", "OAuth " + TOKEN)
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(HttpStatus.CONFLICT.value())
                .withHeader("X-Ticket-Key", key)
        );

        String host = trackerServerMock.remoteAddress().getHostString();
        int port = trackerServerMock.remoteAddress().getPort();
        TrackerClient client = new TrackerClientImpl(5000, 5000, 5000, 2, 1000,
                "http://" + host + ":" + port, TOKEN, "d");
        TrackerCreateTicketDto body = new TrackerCreateTicketDto(QUEUE, "Test summary",
                "desc", "username", List.of(45989L), List.of(), UUID.randomUUID().toString());

        Result<TrackerCreateTicketResponseDto> result = client.createTicket(body).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(key, result.match(TrackerCreateTicketResponseDto::getKey, error -> null));
        trackerServerMock.reset();
    }

    @Test
    public void testCreateTicketError(MockServerClient trackerServerMock) throws JsonProcessingException {
        String errorMessage = "Requested action for /v2/issues was not found";
        TrackerErrorDto responseMock = new TrackerErrorDto(Map.of(), List.of(errorMessage), 404);
        trackerServerMock.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath("/v2/issues")
                        .withHeader("Authorization", "OAuth " + TOKEN)
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(HttpStatus.NOT_FOUND.value())
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(TrackerErrorDto.class)
                        .writeValueAsString(responseMock))
        );

        String host = trackerServerMock.remoteAddress().getHostString();
        int port = trackerServerMock.remoteAddress().getPort();
        TrackerClient client = new TrackerClientImpl(5000, 5000, 5000, 2, 1000,
                "http://" + host + ":" + port, TOKEN, "d");
        TrackerCreateTicketDto body = new TrackerCreateTicketDto(QUEUE, "Test summary",
                "desc", "username", List.of(45989L), List.of(), UUID.randomUUID().toString());

        Result<TrackerCreateTicketResponseDto> result = client.createTicket(body).block();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isFailure());
        Set<TypedError> errors = result.match(u -> null, ErrorCollection::getErrors);
        Assertions.assertNotNull(errors);
        Optional<TypedError> error = errors.stream().findFirst();
        Assertions.assertTrue(error.isPresent());
        Assertions.assertTrue(error.get().getError().contains(errorMessage));
        trackerServerMock.reset();
    }
}
