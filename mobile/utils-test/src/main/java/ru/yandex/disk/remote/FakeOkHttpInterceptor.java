package ru.yandex.disk.remote;

import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BasicResponseBody;
import okio.Buffer;

import javax.annotation.NonnullByDefault;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@NonnullByDefault
public class FakeOkHttpInterceptor implements Interceptor {
    private static final Headers NO_HEADERS = Headers.of(Collections.emptyMap());
    private static final BasicResponseBody NO_BODY = new BasicResponseBody("");
    private final Response.Builder responseBuilder;

    private final List<Request> requests;
    private final List<String> requestBodies;

    private final Queue<ResponseInfo> responses;
    private final ResponseTracker responseTracker;

    public FakeOkHttpInterceptor() {
        responseBuilder = new Response.Builder().protocol(Protocol.HTTP_1_0);
        requests = new ArrayList<>();
        requestBodies = new ArrayList<>();
        responses = new LinkedList<>();
        responseTracker = new ResponseTracker();
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        requests.add(request);

        if (responses.isEmpty()) {
            throw new IllegalStateException("no mock response for " + request);
        }

        final ResponseInfo response = responses.remove();
        if (response.throwOnExecute != null) {
            response.throwOnExecute.fillInStackTrace();
            throw response.throwOnExecute;
        }
        if (response.throwOnExecuteRuntime != null) {
            response.throwOnExecuteRuntime.fillInStackTrace();
            throw response.throwOnExecuteRuntime;
        }

        final Buffer buffer = new Buffer();
        final RequestBody body = request.body();
        if (body != null) {
            body.writeTo(buffer);
            requestBodies.add(new String(ByteStreams.toByteArray(buffer.inputStream())));
        } else {
            requestBodies.add(null);
        }

        responseTracker.track(response.response);

        return responseBuilder
                .request(request)
                .code(response.code)
                .message("FAKE")
                .headers(response.headers)
                .body(response.response).build();
    }

    public void addResponse(final int code) {
        addResponse(code, "");
    }

    public void addResponse(final String body) {
        addResponse(200, body);
    }

    public void addResponsePermanentlyRedirect(final String location) {
        addResponseWithHeader(301, "Location", location);
    }

    public void addResponseTemporarilyRedirect(final String location) {
        addResponseWithHeader(302, "Location", location);
    }

    public void addResponseWithHeader(final int code, final String header, final String value) {
        final Headers headers = new Headers.Builder().add(header, value).build();
        addResponseWithHeaders(code, headers);
    }

    public void addResponseWithHeaders(final int code, final Headers headers) {
        final ResponseInfo response = new ResponseInfo();
        response.code = code;
        response.headers = headers;
        responses.add(response);
    }

    public void addResponse(final int code, final String body) {
        final ResponseInfo response = new ResponseInfo();
        response.code = code;
        response.response = new BasicResponseBody(body);
        responses.add(response);
    }

    public void addResponse(final int code, final String mediaType, final String body) {
        final ResponseInfo response = new ResponseInfo();
        response.code = code;
        response.response = new BasicResponseBody(body) {
            @Override
            public MediaType contentType() {
                return MediaType.parse(mediaType);
            }
        };
        responses.add(response);
    }

    public void addResponse(final int code, final byte[] body, final Headers headers) {
        final ResponseInfo response = new ResponseInfo();
        response.code = code;
        response.response = new BasicResponseBody(body);
        response.headers = headers;
        responses.add(response);
    }

    public Request getRequest() {
        checkHasRequests();
        return requests.get(0);
    }

    private void checkHasRequests() {
        if (requests.isEmpty()) {
            throw new IllegalStateException("no requests");
        }
    }

    public int getResponsesLeft() {
        return responses.size();
    }

    public Request getRequest(final int position) {
        checkHasRequests();
        return requests.get(position);
    }

    public String getRequestBody() {
        checkHasRequests();
        final String body = requestBodies.get(0);
        if (body == null) {
            throw new NullPointerException("no body");
        }
        return body;
    }

    public Map<String, String> getRequestQuery() {
        return getRequestQuery(0);
    }

    public Map<String, String> getRequestQuery(final int position) {
        final Request secondRequest = requests.get(position);
        return parseQuery(secondRequest.url().url().getQuery());
    }

    public void throwIOException() {
        final ResponseInfo response = new ResponseInfo();
        response.throwOnExecute = new IOException("test");
        responses.add(response);
    }

    public void addBadResponse() {
        addResponse(200, "{ { TEST BAD RESPONSE { { {");
    }

    public void throwRuntimeException() {
        final ResponseInfo response = new ResponseInfo();
        response.throwOnExecuteRuntime = new RuntimeException("some parse exception");
        responses.add(response);
    }

    private static Map<String, String> parseQuery(@Nullable final String query) {
        if (query != null) {
            return Splitter.on('&').trimResults().withKeyValueSeparator("=").split(query);
        } else {
            return Collections.emptyMap();
        }
    }

    public void checkBodyClosed() {
        responseTracker.checkState();
    }

    public void throwIOExceptionDuringRead(final int statusCode) {
        final ResponseInfo response = new ResponseInfo();
        response.code = statusCode;
        response.response = new BasicResponseBody("") {
            @Override
            protected long read(final Buffer sink, final long byteCount) throws IOException {
                throw new IOException("unreadable body");
            }
        };
        responses.add(response);
    }

    private class ResponseInfo {
        int code;
        BasicResponseBody response = NO_BODY;
        @Nullable
        IOException throwOnExecute;
        @Nullable
        RuntimeException throwOnExecuteRuntime;
        Headers headers = NO_HEADERS;
    }
}
