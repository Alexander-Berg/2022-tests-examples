package ru.yandex.general.step;

import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.yandex.general.api.GraphqlApiService;
import ru.yandex.general.api.UnsafeOkHttpClient;
import ru.yandex.general.beans.graphql.Data;
import ru.yandex.general.beans.graphql.Request;

import java.io.IOException;

public class RetrofitSteps {

    private static final String GRAPHQL_TEST_HOST = "http://general-gateway-api.vrts-slb.test.vertis.yandex.net:80";

    private Retrofit.Builder retrofit;

    public RetrofitSteps() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);


        OkHttpClient.Builder httpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient().newBuilder();
        httpClient.addInterceptor(logging);
        httpClient.addInterceptor(new AllureOkHttp3());

        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build());
    }

    public Data graphqlPost(Request request) {
        try {
            return retrofit.baseUrl(GRAPHQL_TEST_HOST).build()
                    .create(GraphqlApiService.class)
                    .post(request)
                    .execute().body().getData();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось выполнить запрос в GRAPHQL", e);
        }
    }

}
