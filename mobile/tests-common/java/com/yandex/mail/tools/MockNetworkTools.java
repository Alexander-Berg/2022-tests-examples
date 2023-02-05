package com.yandex.mail.tools;

import com.yandex.mail.util.NonInstantiableException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import androidx.annotation.NonNull;
import kotlin.collections.MapsKt;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

public final class MockNetworkTools {

    private MockNetworkTools() {
        throw new NonInstantiableException();
    }

    @NonNull
    public static MockResponse getOkResponse(@NonNull Map<String, Object> headers, @NonNull byte[] bytes, boolean gzipped) {
        try {
            MockResponse response = new MockResponse();
            if (gzipped) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                GZIPOutputStream gos = new GZIPOutputStream(os);
                gos.write(bytes);
                gos.close();
                os.close();
                bytes = os.toByteArray();
                response.addHeader("Content-Encoding", "gzip");
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    response.addHeader(header.getKey(), header.getValue());
                }
            }
            okio.Buffer buffer = new okio.Buffer();
            buffer.readFrom(new ByteArrayInputStream(bytes));
            response.setBody(buffer);
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static MockResponse getOkResponse(@NonNull byte[] bytes, boolean gzipped) {
        return getOkResponse(Collections.emptyMap(), bytes, gzipped);
    }

    @NonNull
    public static MockResponse getOkResponse(@NonNull String body) {
        return getOkResponse(body.getBytes(), false);
    }

    @NonNull
    public static MockResponse getNotFoundResponse() {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @NonNull
    public static Map<String, String> getEncodedParams(@NonNull String body) {
        StringTokenizer tokenizer = new StringTokenizer(body, "&=");
        Map<String, String> map = new HashMap<>();
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken();
            String value = tokenizer.nextToken();
            map.put(name, value);
        }
        return map;
    }

    @NonNull
    public static Map<String, String> getDecodedParams(@NonNull String body) {
        return MapsKt.mapValues(getEncodedParams(body), s -> {
            try {
                return URLDecoder.decode(s.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @NonNull
    public static Map<String, String> getEncodedParams(@NonNull RecordedRequest request) {
        return getEncodedParams(request.getBody().readUtf8());
    }

    @NonNull
    public static Map<String, String> getDecodedParams(@NonNull RecordedRequest request) {
        return getDecodedParams(request.getBody().readUtf8());
    }

    @NonNull
    public static Map<String, String> getGetParams(@NonNull RecordedRequest request) {
        HttpUrl requestUrl = request.getRequestUrl();
        Map<String, String> map = new HashMap<>();
        for (String key : requestUrl.queryParameterNames()) {
            map.put(key, requestUrl.queryParameter(key));
        }
        return map;
    }

    @NonNull
    public static String getMethod(@NonNull RecordedRequest request) {
        final List<String> pathSegments = request.getRequestUrl().pathSegments();
        return pathSegments.get(pathSegments.size() - 1);
    }
}
