package ru.auto.tests.passport.client;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PassportApiClient {

    private OkHttpClient.Builder clientBuilder;
    private Retrofit.Builder retrofitBuilder;

    public PassportApiClient() {
        clientBuilder = new OkHttpClient.Builder();

        retrofitBuilder = new Retrofit
                .Builder()
                .addConverterFactory(GsonConverterFactory.create());
    }

    public <T> T createService(Class<T> serviceClass) {
        return retrofitBuilder.client(clientBuilder.build())
                .build()
                .create(serviceClass);
    }

    public Retrofit.Builder getRetrofitBuilder() {
        return retrofitBuilder;
    }

    public PassportApiClient setRetrofitBuilder(Retrofit.Builder retrofitBuilder) {
        this.retrofitBuilder = retrofitBuilder;
        return this;
    }

    public void setClientBuilder(OkHttpClient clientBuilder) {
        this.clientBuilder = clientBuilder.newBuilder();
    }
}
