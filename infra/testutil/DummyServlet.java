package ru.yandex.infra.controller.testutil;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

// Saves request parameters and responds with given response
// HttpServletRequest is nullified after handling, so store necessary parts separately
public class DummyServlet extends HttpServlet {
    private volatile CompletableFuture<Map<String, String>> requestParameters = new CompletableFuture<>();
    private volatile CompletableFuture<Map<String, String>> headers = new CompletableFuture<>();
    private final String response;

    public DummyServlet(String response) {
        this.response = response;
    }

    public CompletableFuture<Map<String, String>> getRequestParameters() {
        return requestParameters;
    }

    public CompletableFuture<Map<String, String>> getHeaders() {
        return headers;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(response);
        // Assume all parameters are single-value to simplify client code
        Map<String, String> parameters = EntryStream.of(req.getParameterMap())
                .mapValues(array -> array[0])
                .toMap();
        requestParameters.complete(parameters);
        Map<String, String> headersMap = StreamEx.of(Collections.list(req.getHeaderNames()))
                .toMap(req::getHeader);
        headers.complete(headersMap);
    }

}
