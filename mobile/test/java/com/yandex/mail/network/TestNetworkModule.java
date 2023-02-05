package com.yandex.mail.network;

import com.yandex.mail.storage.preferences.DeveloperSettings;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static org.mockito.Mockito.spy;

public class TestNetworkModule extends NetworkModule {

    /**
     * Default {@link Interceptor}  for okHttp to test retrofit requests by capturing them
     *
     * @see RetrofitMailApiTest
     */
    @NonNull
    public static final Interceptor OK_HTTP_INTERCEPTOR = spy(new InterceptorImpl());

    @Override
    @NonNull
    protected OkHttpClient.Builder getOkHttpBuilder(
            @NonNull OkHttpClient.Builder builder,
            @NonNull Interceptor interceptor,
            @NonNull DeveloperSettings settings
    ) {
        return super.getOkHttpBuilder(builder, interceptor, settings)
                .addInterceptor(OK_HTTP_INTERCEPTOR);
    }

    public static class InterceptorImpl implements Interceptor {

        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            return chain.proceed(chain.request());
        }
    }
}
