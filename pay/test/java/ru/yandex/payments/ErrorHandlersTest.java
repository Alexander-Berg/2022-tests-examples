package ru.yandex.payments;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.exceptions.ContentLengthExceededException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.validation.Validated;
import io.micronaut.web.router.exceptions.DuplicateRouteException;
import io.micronaut.web.router.exceptions.UnsatisfiedRouteException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.payments.server.error.ErrorCode;
import ru.yandex.payments.server.error.ServerErrorException;
import ru.yandex.payments.server.error.StandardErrorCode;

import static java.util.Collections.emptyList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.payments.server.error.StandardErrorCode.CONSTRAINT_VIOLATION;
import static ru.yandex.payments.server.error.StandardErrorCode.CONTENT_LENGTH_EXCEEDED;
import static ru.yandex.payments.server.error.StandardErrorCode.CONVERSION_ERROR;
import static ru.yandex.payments.server.error.StandardErrorCode.DUPLICATE_ROUTE;
import static ru.yandex.payments.server.error.StandardErrorCode.INVALID_JSON;
import static ru.yandex.payments.server.error.StandardErrorCode.UNSATISFIED_ARGUMENT;
import static ru.yandex.payments.server.error.StandardErrorCode.UNSATISFIED_ROUTE;
import static ru.yandex.payments.server.error.StandardErrorCode.URI_SYNTAX_ERROR;

@Getter
@AllArgsConstructor
enum CustomServerErrorCode implements ErrorCode<CustomServerErrorCode> {
    ITS_OKAY(HttpStatus.I_AM_A_TEAPOT);

    private final HttpStatus status;
}

@Introspected
record AdditionalErrorInfo(String description,
                           int value) {
    AdditionalErrorInfo() {
        this("description", 42);
    }
}

class CustomServerException extends ServerErrorException {
    CustomServerException() {
        super(CustomServerErrorCode.ITS_OKAY, "Good news, everyone", Optional.of(new AdditionalErrorInfo()));
    }

    @Override
    public Map<CharSequence, CharSequence> getExtraHeaders() {
        return Map.of("Custom-Header", "custom-value");
    }
}

@Validated
@Controller
class ErrorController {
    @Get("/constraint_violation")
    String constraintViolation(@Min(0) @Max(0) @QueryValue int value) {
        return "";
    }

    @Get("/content_length_exceeded")
    String contentLengthExceeded() {
        throw new ContentLengthExceededException(10, 100);
    }

    @Get("/conversion_error")
    String conversionError() {
        throw new ConversionErrorException(Argument.of(String.class), new IllegalArgumentException("err"));
    }

    @Get("/duplicate_route")
    String duplicateRoute() {
        throw new DuplicateRouteException("/route", emptyList());
    }

    @SneakyThrows
    @Get("/json_processing_error")
    String jsonProcessingError() {
        throw new JsonGenerationException("message", (JsonGenerator) null);
    }

    @Get("/unsatisfied_argument")
    String unsatisfiedArg() {
        throw new UnsatisfiedArgumentException(Argument.of(String.class), "message");
    }

    @Get("/unsatisfied_route")
    String unsatisfiedRoute() {
        throw UnsatisfiedRouteException.create(Argument.of(String.class));
    }

    @SneakyThrows
    @Get("/uri_syntax_error")
    String uriSyntaxError() {
        throw new URISyntaxException("/input/uri", "some reason");
    }

    @Get("/server_error")
    String serverError() {
        throw new CustomServerException();
    }
}

@Client(value = "/", errorType = String.class)
interface ErrorClient {
    @Get("/constraint_violation?value=100")
    String constraintViolation();

    @Get("/content_length_exceeded")
    String contentLengthExceeded();

    @Get("/conversion_error")
    String conversionError();

    @Get("/duplicate_route")
    String duplicateRoute();

    @Get("/json_processing_error")
    String jsonProcessingError();

    @Get("/unsatisfied_argument")
    String unsatisfiedArg();

    @Get("/unsatisfied_route")
    String unsatisfiedRoute();

    @Get("/uri_syntax_error")
    String uriSyntaxError();

    @Get("/server_error")
    String serverError();
}

@MicronautTest
class ErrorHandlersTest {
    @Inject
    ErrorClient client;

    private static Arguments arg(StandardErrorCode code, Consumer<ErrorClient> trigger) {
        return Arguments.of(code, trigger);
    }

    static Stream<Arguments> testArguments() {
        return Stream.of(
                arg(CONSTRAINT_VIOLATION, ErrorClient::constraintViolation),
                arg(CONTENT_LENGTH_EXCEEDED, ErrorClient::contentLengthExceeded),
                arg(CONVERSION_ERROR, ErrorClient::conversionError),
                arg(DUPLICATE_ROUTE, ErrorClient::duplicateRoute),
                arg(INVALID_JSON, ErrorClient::jsonProcessingError),
                arg(UNSATISFIED_ARGUMENT, ErrorClient::unsatisfiedArg),
                arg(UNSATISFIED_ROUTE, ErrorClient::unsatisfiedRoute),
                arg(URI_SYNTAX_ERROR, ErrorClient::uriSyntaxError)
        );
    }

    @MethodSource("testArguments")
    @ParameterizedTest(name = "Error = {0}")
    @DisplayName("Verify that error handlers produces expected output")
    void testErrorResponse(StandardErrorCode expectedErrorCode, Consumer<ErrorClient> trigger) {
        try {
            trigger.accept(client);
            fail("Error not found");
        } catch (HttpClientResponseException e) {
            val response = e.getResponse();
            assertThat((Object) response.getStatus())
                    .isEqualTo(expectedErrorCode.getStatus());

            val body = response.getBody(String.class);
            assertThat(body)
                    .isPresent();

            assertThatJson(body.get())
                    .isObject()
                    .containsEntry("code", BigDecimal.valueOf(expectedErrorCode.getCode()))
                    .containsKey("message");
        }
    }

    @Test
    @DisplayName("Verify that custom server error contains all possible information")
    void testCustomServerErrorResponse() {
        try {
            client.serverError();
            fail("Server error not found");
        } catch (HttpClientResponseException e) {
            val response = e.getResponse();
            assertThat((Object) response.getStatus())
                    .isEqualTo(HttpStatus.I_AM_A_TEAPOT);

            val body = response.getBody(String.class);
            assertThat(body)
                    .isPresent();

            assertThatJson(body.get())
                    .isObject()
                    .containsEntry("code", BigDecimal.valueOf(CustomServerErrorCode.ITS_OKAY.getCode()))
                    .containsEntry("message", "Good news, everyone");

            assertThatJson(body.get())
                    .inPath("info")
                    .isObject()
                    .containsExactly(
                            entry("description", "description"),
                            entry("value", BigDecimal.valueOf(42))
                    );

            assertThat(response.header("Custom-Header"))
                    .isNotNull()
                    .isEqualTo("custom-value");
        }
    }
}
