package ru.auto.tests.salesman.user.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import org.aeonbits.owner.ConfigFactory;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import ru.auto.test.salesman.user.ApiClient;
import ru.auto.test.salesman.user.JSON;
import ru.auto.tests.salesman.user.adaptor.SalesmanUserApiAdaptor;
import ru.auto.tests.salesman.user.config.SalesmanUserApiConfig;

import java.time.OffsetDateTime;
import java.util.Date;

public class SalesmanUserApiModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new SalesmanUserApiAdaptor());
    }

    @Provides
    @Singleton
    public SalesmanUserApiConfig provideConfig() {
        return ConfigFactory.create(SalesmanUserApiConfig.class, System.getProperties(), System.getenv());
    }

    @Provides
    @Singleton
    public ApiClient providesApiSalesmanUser(SalesmanUserApiConfig config) {
        final OkHttpClient client = new OkHttpClient().newBuilder().addInterceptor(new AllureOkHttp3()).build();
        ApiClient apiClient = new ApiClient();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new JSON.DateTypeAdapter())
                .registerTypeAdapter(java.sql.Date.class, new JSON.SqlDateTypeAdapter())
                .registerTypeAdapter(OffsetDateTime.class, new JSON.OffsetDateTimeTypeAdapter())
                .create();

        Retrofit.Builder builder = new Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(config.getSalesmanUserUrl());

        apiClient.setAdapterBuilder(builder);
        apiClient.configureFromOkclient(client);
        return apiClient;
    }
}
