/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.api.retrofit;

import com.yandex.datasync.Config;
import com.yandex.datasync.LogLevel;
import com.yandex.datasync.internal.api.Api;
import com.yandex.datasync.internal.api.ApiFactory;
import com.yandex.datasync.internal.api.HttpStatusCode;
import com.yandex.datasync.internal.api.exceptions.BaseException;
import com.yandex.datasync.internal.api.exceptions.NetworkException;
import com.yandex.datasync.internal.api.exceptions.http.BadRequestException;
import com.yandex.datasync.internal.api.exceptions.http.ConflictException;
import com.yandex.datasync.internal.api.exceptions.http.ForbiddenException;
import com.yandex.datasync.internal.api.exceptions.http.GoneException;
import com.yandex.datasync.internal.api.exceptions.HttpErrorException;
import com.yandex.datasync.internal.api.exceptions.http.InsufficientStorageException;
import com.yandex.datasync.internal.api.exceptions.http.LockedException;
import com.yandex.datasync.internal.api.exceptions.http.NotAcceptableException;
import com.yandex.datasync.internal.api.exceptions.http.NotFoundException;
import com.yandex.datasync.internal.api.exceptions.http.PreconditionFailedException;
import com.yandex.datasync.internal.api.exceptions.http.ResourceNotFoundException;
import com.yandex.datasync.internal.api.exceptions.http.TooManyRequestsException;
import com.yandex.datasync.internal.api.exceptions.http.UnauthorizedException;
import com.yandex.datasync.internal.api.exceptions.http.UnsupportedMediaTypeException;
import com.yandex.datasync.Credentials;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.util.NetworkSecurityPolicyShadow;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@org.robolectric.annotation.Config(shadows = NetworkSecurityPolicyShadow.class)
public class ErrorHandlerInterceptorTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private static final String MOCK_TOKEN = "mock_token";

    private static final String MOCK_USER_ID = "mock_user_id";

    private MockWebServer mockWebServer;

    private Api api;

    private String errorText;

    @Before
    public void setUp() throws Exception {

        errorText = ResourcesUtil.getTextFromFile("error.json");

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final Config.Builder builder = new Config.Builder()
                .baseUrl(mockWebServer.url("/").toString())
                .credentials(new Credentials(MOCK_USER_ID, MOCK_TOKEN))
                .logLevel(LogLevel.DEBUG);

        api = ApiFactory.create(builder.build());
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test(expected = BadRequestException.class)
    public void testBadRequestException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.BAD_REQUEST);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = UnauthorizedException.class)
    public void testNotAuthorizedException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.UNAUTHORIZED);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testResourceNotFoundException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.RESOURCE_NOT_FOUND);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = ForbiddenException.class)
    public void testForbiddenException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.FORBIDDEN);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = NotFoundException.class)
    public void testNotFoundException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.NOT_FOUND);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = NotAcceptableException.class)
    public void testNotAcceptableException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.NOT_ACCEPTABLE);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = ConflictException.class)
    public void testConflictException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.CONFLICT);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = GoneException.class)
    public void testGoneException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.GONE);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = PreconditionFailedException.class)
    public void testPreconditionFailedException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.PRECONDITION_FAILED);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = UnsupportedMediaTypeException.class)
    public void testUnsupportedMediaTypeException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = LockedException.class)
    public void testLockedException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.LOCKED);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = TooManyRequestsException.class)
    public void testTooManyRequestsException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.TOO_MANY_REQUESTS);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = InsufficientStorageException.class)
    public void testInsufficientStorageException() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.INSUFFICIENT_STORAGE);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = HttpErrorException.class)
    public void testEmptyBodyWithExpectedError() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.INSUFFICIENT_STORAGE);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = HttpErrorException.class)
    public void testXmlResponseBody() throws Exception {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(HttpStatusCode.INSUFFICIENT_STORAGE);
        final String errorText = ResourcesUtil.getTextFromFile("error.xml");
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = HttpErrorException.class)
    public void testUnexpectedHttpErrorWithErrorBody() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(499);
        mockResponse.setBody(errorText);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = HttpErrorException.class)
    public void testUnexpectedHttpErrorWithoutErrorBody() throws BaseException {
        final MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(499);

        mockWebServer.enqueue(mockResponse);

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }

    @Test(expected = NetworkException.class)
    public void testConnectionException() throws Exception {
        mockWebServer.shutdown();

        api.removeDatabase(MOCK_CONTEXT, MOCK_DATABASE_ID);
    }
}