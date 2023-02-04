package ru.auto.tests.passport.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import ru.auto.tests.passport.client.PassportApiClient;
import ru.auto.tests.passport.client.SlowDownInterceptor;
import ru.auto.tests.passport.config.PassportConfig;

public class PassportYandexModule extends AbstractModule {

    @Provides
    @Singleton
    public PassportApiClient providesYandexPassport(PassportConfig config) {
        final OkHttpClient client = new OkHttpClient().newBuilder().addInterceptor(new SlowDownInterceptor())
                .addInterceptor(new AllureOkHttp3()).build();
        PassportApiClient passportApiClient = new PassportApiClient();
        Retrofit.Builder builder = new PassportApiClient().getRetrofitBuilder()
                .baseUrl(config.getYandexPassportInternalTestUrl());
        passportApiClient.setRetrofitBuilder(builder);
        passportApiClient.setClientBuilder(client);
        return passportApiClient;
    }

    @Override
    protected void configure() {
    }
}
