package ru.auto.tests.vos2.provider;

import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.auto.tests.vos2.VosClientRetrofit;
import ru.auto.tests.vos2.VosConfig;

import javax.inject.Inject;
import javax.inject.Provider;

public class VosClientProvider implements Provider<VosClientRetrofit> {

    @Inject
    private VosConfig config;

    protected VosConfig getConfig() {
        return config;
    }

    @Override
    public VosClientRetrofit get() {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(new AllureOkHttp3())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(getConfig().getBaseUrl() + ":" + getConfig().getPort())
                .client(client)
                .build();

        return retrofit.create(VosClientRetrofit.class);
    }
}
