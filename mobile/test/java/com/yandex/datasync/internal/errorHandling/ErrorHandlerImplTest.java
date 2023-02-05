/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.errorHandling;

import com.yandex.datasync.ErrorType;
import com.yandex.datasync.internal.api.exceptions.HttpErrorException;
import com.yandex.datasync.internal.api.exceptions.http.BadRequestException;
import com.yandex.datasync.internal.api.exceptions.http.ConflictException;
import com.yandex.datasync.internal.api.exceptions.http.ForbiddenException;
import com.yandex.datasync.internal.api.exceptions.http.GoneException;
import com.yandex.datasync.internal.api.exceptions.http.InsufficientStorageException;
import com.yandex.datasync.internal.api.exceptions.http.LockedException;
import com.yandex.datasync.internal.api.exceptions.http.NotAcceptableException;
import com.yandex.datasync.internal.api.exceptions.http.NotFoundException;
import com.yandex.datasync.internal.api.exceptions.http.PreconditionFailedException;
import com.yandex.datasync.internal.api.exceptions.http.ResourceNotFoundException;
import com.yandex.datasync.internal.api.exceptions.http.TooManyRequestsException;
import com.yandex.datasync.internal.api.exceptions.http.UnauthorizedException;
import com.yandex.datasync.internal.api.exceptions.http.UnsupportedMediaTypeException;
import com.yandex.datasync.internal.model.response.ErrorResponse;
import com.yandex.datasync.wrappedModels.Error;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.yandex.datasync.asserters.ErrorAsserter.assertError;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ErrorHandlerImplTest {

    private static final int MOCK_HTTP_ERROR_CODE = 404;

    private static final String MOCK_HTTP_ERROR_MSG = "mock_http_error_msg";

    @Mock
    private ErrorResponse errorResponse;

    @Before
    public void setUp() {
        initMocks(this);
        when(errorResponse.toString()).thenReturn(MOCK_HTTP_ERROR_MSG);
    }

    @Test
    public void testHttpErrorHandle() {

        final HttpErrorException exception =
                new HttpErrorException(MOCK_HTTP_ERROR_CODE, MOCK_HTTP_ERROR_MSG);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_UNEXPECTED_ERROR, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testBadRequestErrorHandle() {

        final BadRequestException exception = new BadRequestException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_BAD_REQUEST, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testConflictErrorHandle() {

        final ConflictException exception = new ConflictException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_CONFLICT, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testForbiddenErrorHandle() {

        final ForbiddenException exception = new ForbiddenException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_FORBIDDEN, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testGoneErrorHandle() {

        final GoneException exception = new GoneException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected = new Error(ErrorType.HTTP_GONE, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testInsufficientStorageErrorHandle() {

        final InsufficientStorageException exception = new InsufficientStorageException(
                errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_INSUFFICIENT_STORAGE, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testLockedErrorHandle() {

        final LockedException exception = new LockedException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected = new Error(ErrorType.HTTP_LOCKED, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testNotAcceptableErrorHandle() {

        final NotAcceptableException exception = new NotAcceptableException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_NOT_ACCEPTABLE, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testNotFoundErrorHandle() {

        final NotFoundException exception = new NotFoundException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_NOT_FOUND, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testPreconditionFailedErrorHandle() {

        final PreconditionFailedException exception = new PreconditionFailedException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_PRECONDITION_FAILED, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testResourceNotFoundErrorHandle() {

        final ResourceNotFoundException exception = new ResourceNotFoundException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_RESOURCE_NOT_FOUND, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testTooManyRequestsErrorHandle() {

        final TooManyRequestsException exception = new TooManyRequestsException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_TOO_MANY_REQUESTS, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testUnauthorizedErrorHandle() {

        final UnauthorizedException exception = new UnauthorizedException(errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_UNAUTHORIZED, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }

    @Test
    public void testUnsupportedMediaTypeErrorHandle() {

        final UnsupportedMediaTypeException exception = new UnsupportedMediaTypeException(
                errorResponse);

        final ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

        final Error actual = errorHandler.handleError(exception);
        final Error expected =
                new Error(ErrorType.HTTP_UNSUPPORTED_MEDIA_TYPE, MOCK_HTTP_ERROR_MSG);

        assertError(actual, expected);
    }
}