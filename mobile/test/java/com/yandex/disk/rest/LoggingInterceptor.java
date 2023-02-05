/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.util.List;

import com.yandex.disk.rest.util.Log;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public class LoggingInterceptor implements Interceptor {

    private static final String TAG = "LoggingInterceptor";

    private static final String SEND_PREFIX = " >> ";
    private static final String RECEIVE_PREFIX = " << ";

    private boolean logWire;

    public LoggingInterceptor(boolean logWire) {
        this.logWire = logWire;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String hash = Integer.toHexString(chain.hashCode());
        String sendPrefix = hash + SEND_PREFIX;
        String receivePrefix = hash + RECEIVE_PREFIX;

        if (logWire) {
            RequestBody requestBody = request.body();
            if (requestBody != null) {
                Log.info(TAG,sendPrefix + "request: " + requestBody);
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                byte[] requestBuffer = ByteStreams.toByteArray(buffer.inputStream());
                logBuffer(sendPrefix, requestBuffer);
            }
            request = request.newBuilder()
                    .removeHeader("Accept-Encoding")
                    .addHeader("Accept-Encoding", "")
                    .build();
        }

        Log.info(TAG,sendPrefix + request.method() + " " + request.url());
        Log.info(TAG,sendPrefix + "on " + chain.connection());
        logHeaders(sendPrefix, request.headers());

        Response response = chain.proceed(request);
        Log.info(TAG,receivePrefix + response.protocol() + " " + response.code()
                + " " + response.message());
        logHeaders(receivePrefix, response.headers());

        if (logWire) {
            ResponseBody body = response.body();
            byte[] responseBuffer = ByteStreams.toByteArray(body.byteStream());
            response = response.newBuilder()
                    .body(ResponseBody.create(body.contentType(), responseBuffer))
                    .build();
            logBuffer(receivePrefix, responseBuffer);
        }

        return response;
    }

    private void logBuffer(String prefix, byte[] buf) {
        if (buf.length < 10240) {       // assume binary output: magic number from RestClientTest
            Log.info(TAG,prefix + new String(buf));
        } else {
            Log.info(TAG,prefix + "[" + buf.length + " bytes]");
        }
    }

    private void logHeaders(String prefix, Headers headers) {
        for (String name : headers.names()) {
            List<String> values = headers.values(name);
            for (String value : values) {
                Log.info(TAG,prefix + name + ": " + value);
            }
        }
    }
}