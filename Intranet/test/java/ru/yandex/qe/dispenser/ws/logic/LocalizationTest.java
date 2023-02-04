package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collections;
import java.util.Locale;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.domain.exception.ExceptionType;
import ru.yandex.qe.dispenser.domain.exception.MultiMessageException;
import ru.yandex.qe.dispenser.domain.exception.SingleMessageException;
import ru.yandex.qe.dispenser.domain.hierarchy.Session;
import ru.yandex.qe.dispenser.domain.i18n.LocalizableString;
import ru.yandex.qe.dispenser.domain.util.Errors;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;
import ru.yandex.qe.dispenser.ws.param.DiExceptionMapper;

public class LocalizationTest extends AcceptanceTestBase {

    @Autowired
    @Qualifier("errorMessageSource")
    private MessageSource errorMessageSource;

    @Autowired
    private DiExceptionMapper exceptionMapper;

    private static final String ACCEPT_LANGUAGE = "Accept-Language";
    private static final String RU = "ru";
    private static final String EN = "en";

    public static Stream<Object[]> types() {
        return Stream.of(ExceptionType.values())
                .map(t -> new Object[] {t});
    }


    private static Body.BodyBuilder requestBuilder() {
        return BaseQuotaRequestTest.requestBodyBuilderWithDefaultFields()
                .projectKey("yandex")
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .changes(NIRVANA, CPU, null, Collections.emptySet(), DiAmount.of(1, DiUnit.CORES));
    }

    @Test
    public void checkIllegalArgumentLocalization() {
        final Body requestBody = requestBuilder().build();
        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/")
                .post(requestBody);
        Assertions.assertTrue(SpyWebClient.lastResponse().contains("No campaign is active at this moment"));

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/")
                .header(ACCEPT_LANGUAGE, EN)
                .post(requestBody);
        Assertions.assertTrue(SpyWebClient.lastResponse().contains("No campaign is active at this moment"));


        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/")
                .header(ACCEPT_LANGUAGE, RU)
                .post(requestBody);

        Assertions.assertTrue(SpyWebClient.lastResponse().contains("В данный момент нет активных кампаний сбора заявок"));
    }

    @MethodSource("types")
    @ParameterizedTest
    public void singleMessageExceptionCheck(final ExceptionType type) {
        SingleMessageException exception = new SingleMessageException(type, LocalizableString.of("foo.bar", 42));
        Response response = exceptionMapper.toResponse(exception);
        Assertions.assertEquals(response.getStatus(), type.getStatus().getStatusCode());

        String entity = response.readEntity(String.class);

        Assertions.assertTrue(entity.contains("foo.bar"));

        exception = new SingleMessageException(type, LocalizableString.of("invalid.segment.set"));
        response = exceptionMapper.toResponse(exception);
        entity = response.readEntity(String.class);

        Assertions.assertTrue(entity.contains("Invalid segment set"));

        Session.USER_LOCALE.set(new Locale("ru"));

        exception = new SingleMessageException(type, LocalizableString.of("invalid.segment.set"));
        response = exceptionMapper.toResponse(exception);
        entity = response.readEntity(String.class);

        Assertions.assertTrue(entity.contains("Некорректный набор сегментов"));

        Session.USER_LOCALE.remove();
    }

    @MethodSource("types")
    @ParameterizedTest
    public void multiMessageExceptionCheck(final ExceptionType type) {
        Errors<LocalizableString> errors = new Errors<>(ImmutableList.of(LocalizableString.of("foo.bar"), LocalizableString.of("bar.foo")));
        MultiMessageException exception = new MultiMessageException(type, errors);
        Response response = exceptionMapper.toResponse(exception);
        Assertions.assertEquals(response.getStatus(), type.getStatus().getStatusCode());

        String entity = response.readEntity(String.class);

        Assertions.assertTrue(entity.contains("foo.bar\\nbar.foo"));

        errors = new Errors<>(ImmutableList.of(LocalizableString.of("invalid.segment.set"), LocalizableString.of("description.required")));
        exception = new MultiMessageException(type, errors);
        response = exceptionMapper.toResponse(exception);
        entity = response.readEntity(String.class);

        Assertions.assertTrue(entity.contains("Invalid segment set\\nDescription is required"));

        Session.USER_LOCALE.set(new Locale("ru"));

        exception = new MultiMessageException(type, errors);
        response = exceptionMapper.toResponse(exception);
        entity = response.readEntity(String.class);

        Assertions.assertTrue(entity.contains("Некорректный набор сегментов\\nНеобходимо описание"));

        Session.USER_LOCALE.remove();
    }
}
