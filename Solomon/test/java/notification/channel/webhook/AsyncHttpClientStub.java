package ru.yandex.solomon.alert.notification.channel.webhook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ClientStats;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.SignatureCalculator;
import org.asynchttpclient.uri.Uri;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.matchers.HttpRequestMatcher;
import org.mockserver.matchers.MatcherBuilder;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.model.Body;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.NottableString;


/**
 * @author Vladimir Gordiychuk
 */
public class AsyncHttpClientStub implements AsyncHttpClient {

    private final List<HttpRequestMatcher> httpRequestMatchers = new CopyOnWriteArrayList<>();
    private final MatcherBuilder matcherBuilder = new MatcherBuilder(new MockServerLogger());

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public AsyncHttpClient setSignatureCalculator(SignatureCalculator signatureCalculator) {
        return this;
    }

    @Override
    public BoundRequestBuilder prepare(String method, String url) {
        return requestBuilder(method, url);
    }

    @Override
    public BoundRequestBuilder prepareGet(String url) {
        return requestBuilder("GET", url);
    }

    @Override
    public BoundRequestBuilder prepareConnect(String url) {
        return requestBuilder("CONNECT", url);
    }

    @Override
    public BoundRequestBuilder prepareOptions(String url) {
        return requestBuilder("OPTIONS", url);
    }

    @Override
    public BoundRequestBuilder prepareHead(String url) {
        return requestBuilder("HEAD", url);
    }

    @Override
    public BoundRequestBuilder preparePost(String url) {
        return requestBuilder("POST", url);
    }

    @Override
    public BoundRequestBuilder preparePut(String url) {
        return requestBuilder("PUT", url);
    }

    @Override
    public BoundRequestBuilder prepareDelete(String url) {
        return requestBuilder("DELETE", url);
    }

    @Override
    public BoundRequestBuilder preparePatch(String url) {
        return requestBuilder("PATCH", url);
    }

    @Override
    public BoundRequestBuilder prepareTrace(String url) {
        return requestBuilder("TRACE", url);
    }

    @Override
    public BoundRequestBuilder prepareRequest(Request request) {
        return requestBuilder(request);
    }

    @Override
    public BoundRequestBuilder prepareRequest(RequestBuilder requestBuilder) {
        return prepareRequest(requestBuilder.build());
    }

    @Override
    public <T> ListenableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public <T> ListenableFuture<T> executeRequest(RequestBuilder requestBuilder, AsyncHandler<T> handler) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public ListenableFuture<Response> executeRequest(Request request) {
        HttpRequest httpRequest = new HttpRequest()
                .withMethod(request.getMethod())
                .withPath(request.getUrl())
                .withHeaders(request.getHeaders()
                        .entries()
                        .stream()
                        .map(entry -> new Header(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList()))
                .withBody(request.getStringData());

        Expectation expectation = firstMatchingExpectation(httpRequest);
        if (expectation != null) {
            return prepareResult(expectation);
        }

        throw new UnsupportedOperationException("Not found response for request: " + request);
    }

    @Nullable
    private Expectation firstMatchingExpectation(HttpRequest httpRequest) {
        Expectation matchingExpectation = null;
        for (HttpRequestMatcher httpRequestMatcher : httpRequestMatchers) {
            if (httpRequestMatcher.matches(httpRequest, httpRequest)) {
                matchingExpectation = httpRequestMatcher.decrementRemainingMatches();
            }
            if (!httpRequestMatcher.isActive()) {
                httpRequestMatchers.remove(httpRequestMatcher);
            }
            if (matchingExpectation != null) {
                break;
            }
        }
        return matchingExpectation;
    }

    private ListenableFuture<Response> prepareResult(Expectation expectation) {
        CompletableFuture<Response> result =
                CompletableFuture.supplyAsync(() ->
                        new ResponseAdapter(expectation.getHttpResponse())
                );
        return new CompletableFutureToListenable<>(result);
    }

    @Override
    public ListenableFuture<Response> executeRequest(RequestBuilder requestBuilder) {
        return executeRequest(requestBuilder.build());
    }

    @Override
    public ClientStats getClientStats() {
        return new ClientStats(ImmutableMap.of());
    }

    @Override
    public void flushChannelPoolPartitions(Predicate<Object> predicate) {
    }

    @Override
    public AsyncHttpClientConfig getConfig() {
        return new DefaultAsyncHttpClientConfig.Builder()
                .build();
    }

    @Override
    public void close() throws IOException {
    }

    protected BoundRequestBuilder requestBuilder(String method, String url) {
        return new BoundRequestBuilder(this, method, true).setUrl(url);
    }

    protected BoundRequestBuilder requestBuilder(Request prototype) {
        return new BoundRequestBuilder(this, prototype);
    }

    public ChainExpectation when(HttpRequest httpRequest) {
        return when(httpRequest, Times.unlimited());
    }

    public ChainExpectation when(HttpRequest httpRequest, Times times) {
        Expectation expectation = new Expectation(httpRequest, times, TimeToLive.unlimited());
        httpRequestMatchers.add(matcherBuilder.transformsToMatcher(expectation));
        return new ChainExpectation(expectation);
    }

    private class CompletableFutureToListenable<T> implements ListenableFuture<T> {
        private final CompletableFuture<T> future;

        public CompletableFutureToListenable(CompletableFuture<T> future) {
            this.future = future;
        }

        @Override
        public void done() {
            future.cancel(true);
        }

        @Override
        public void abort(Throwable t) {
            future.completeExceptionally(t);
        }

        @Override
        public void touch() {
        }

        @Override
        public ListenableFuture<T> addListener(Runnable listener, Executor exec) {
            future.thenRunAsync(listener, exec);
            return this;
        }

        @Override
        public CompletableFuture<T> toCompletableFuture() {
            return future;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }
    }

    private class ResponseAdapter implements Response {
        private final Optional<HttpResponse> httpResponse;

        public ResponseAdapter(@Nullable HttpResponse httpResponse) {
            this.httpResponse = Optional.ofNullable(httpResponse);
        }

        @Override
        public int getStatusCode() {
            return httpResponse
                    .map(HttpResponse::getStatusCode)
                    .orElse(404);
        }

        @Override
        public String getStatusText() {
            return HttpResponseStatus.valueOf(getStatusCode()).reasonPhrase();
        }

        @Override
        public byte[] getResponseBodyAsBytes() {
            return httpResponse
                    .map(HttpResponse::getBody)
                    .map(Body::getRawBytes)
                    .orElse(new byte[0]);
        }

        @Override
        public ByteBuffer getResponseBodyAsByteBuffer() {
            return ByteBuffer.wrap(getResponseBodyAsBytes());
        }

        @Override
        public InputStream getResponseBodyAsStream() {
            return new ByteArrayInputStream(getResponseBodyAsBytes());
        }

        @Override
        public String getResponseBody(Charset charset) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public String getResponseBody() {
            return httpResponse.map(HttpResponse::getBodyAsString).orElse("");
        }

        @Override
        public Uri getUri() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public String getContentType() {
            return getHeader("content-type");
        }

        @Override
        public String getHeader(CharSequence name) {
            return httpResponse.map(r -> Iterables.getFirst(r.getHeader((String) name), null))
                    .orElse(null);
        }

        @Override
        public List<String> getHeaders(CharSequence name) {
            return httpResponse.map(r -> r.getHeader((String) name))
                    .orElse(Collections.emptyList());
        }

        @Override
        public HttpHeaders getHeaders() {
            return httpResponse.map(r -> {
                HttpHeaders headers = new DefaultHttpHeaders();

                for (Header header : r.getHeaders().getEntries()) {
                    String name = header.getName().getValue();
                    List<String> values = header.getValues()
                            .stream().map(NottableString::getValue)
                            .collect(Collectors.toList());
                    headers.add(name, values);
                }

                return headers;
            }).orElseGet(DefaultHttpHeaders::new);
        }

        @Override
        public boolean isRedirected() {
            return false;
        }

        @Override
        public List<Cookie> getCookies() {
            return httpResponse.map(r -> r.getCookies().getEntries()
                    .stream()
                    .map(cookie -> (Cookie) new DefaultCookie(cookie.getName().getValue(), cookie.getValue().getValue()))
                    .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
        }

        @Override
        public boolean hasResponseStatus() {
            return false;
        }

        @Override
        public boolean hasResponseHeaders() {
            return httpResponse.map(r -> r.getHeaders().isEmpty()).orElse(false);
        }

        @Override
        public boolean hasResponseBody() {
            return httpResponse.map(HttpResponse::getBody).isPresent();
        }

        @Override
        public SocketAddress getRemoteAddress() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public SocketAddress getLocalAddress() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public String toString() {
            return "ResponseAdapter{httpResponse=" + httpResponse + '}';
        }
    }
}
