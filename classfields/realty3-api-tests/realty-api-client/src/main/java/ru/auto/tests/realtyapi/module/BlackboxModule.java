package ru.auto.tests.realtyapi.module;

import com.google.gson.annotations.SerializedName;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import lombok.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.POST;
import retrofit2.http.Query;
import ru.auto.tests.passport.client.SlowDownInterceptor;
import ru.auto.tests.passport.config.PassportConfig;

public class BlackboxModule extends AbstractModule {
    public BlackboxModule() {
    }

    @Provides
    @Singleton
    public BlackBoxApiService providesBlackbox(PassportConfig config) {
        OkHttpClient client = (new OkHttpClient()).newBuilder()
                .addInterceptor(new SlowDownInterceptor())
                .addInterceptor(new AllureOkHttp3())
                .build();
        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(config.getPassTestUrl())
                .client(client)
                .build()
                .create(BlackBoxApiService.class);
    }

    protected void configure() {
    }

    public interface BlackBoxApiService {
        @POST("blackbox?method=oauth&format=json")
        Call<BlackboxResponse> checkOauthToken(@Query("oauth_token") String token, @Query("userip") String userip);
    }

    public static class BlackboxResponse {
        @SerializedName("error")
        @NonNull
        public String error;
    }
}