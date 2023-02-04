package ru.auto.tests.passport.client;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class SlowDownInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request request = chain.request();
        try {
            MILLISECONDS.sleep(200);
        } catch (InterruptedException ignored) {
        }
        return chain.proceed(request);
    }
}
