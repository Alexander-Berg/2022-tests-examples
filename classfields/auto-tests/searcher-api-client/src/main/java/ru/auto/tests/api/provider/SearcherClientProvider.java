package ru.auto.tests.api.provider;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.auto.tests.api.SearcherClient;
import ru.auto.tests.api.SearcherConfig;
import ru.auto.tests.api.logger.AllureLoggingInterceptor;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class SearcherClientProvider implements Provider<SearcherClient> {

    @Inject
    private SearcherConfig config;

    protected SearcherConfig getConfig() {
        return config;
    }

    @Override
    public SearcherClient get() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AllureLoggingInterceptor())
                .build();

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(getConfig().getBaseUrl())
                .client(client)
                .build();

        return retrofit.create(SearcherClient.class);
    }
}
