package ru.auto.tests.passport.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import ru.auto.test.passport.ApiClient;
import ru.auto.tests.passport.config.PassportApiConfig;

/**
 * Created by vicdev on 18.09.17.
 */
public class PassportApiModule extends AbstractModule {

    @Provides
    @Singleton
    public ApiClient providesApiVerticalsPassport(PassportApiConfig config) {
        final OkHttpClient client = new OkHttpClient().newBuilder().addInterceptor(new AllureOkHttp3()).build();
        ApiClient apiClient = new ApiClient();
        Retrofit.Builder builder = new ApiClient().getAdapterBuilder().baseUrl(config.getPassportUrl());
        apiClient.setAdapterBuilder(builder);
        apiClient.configureFromOkclient(client);
        return apiClient;
    }

    @Override
    protected void configure() {
    }
}
