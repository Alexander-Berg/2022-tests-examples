package ru.yandex.general.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import lombok.experimental.Accessors;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.yandex.general.api.TusApiService;
import ru.yandex.general.api.UnsafeOkHttpClient;
import ru.yandex.general.beans.tus.AccountResponse;
import ru.yandex.general.beans.tus.CreateAccountResponse;
import ru.yandex.general.config.GeneralWebConfig;
import ru.yandex.general.consts.Accounts.AccountsForOfferCreation;

import java.io.IOException;
import java.util.Random;

import static java.lang.String.format;

@Accessors(chain = true)
public class PassportSteps {

    private static final String TUS_HOST = "https://tus.yandex-team.ru/";
    private static final String COMMON_LOGIN = "yandex-team-64297.20224";
    private static final String COMMON_PASSWORD = "zTTV.JaEY";
    private static final String WITH_OFFERS_LOGIN = "yandex-team-58899.92916";
    private static final String WITH_OFFERS_PASSWORD = "fmrg.LhTg";

    private Retrofit.Builder retrofit;
    private AccountsForOfferCreation currentAccount;

    @Inject
    private GeneralWebConfig config;

    @Inject
    private UrlSteps urlSteps;

    @Step("Создаем аккаунт и авторизуемся")
    public void createAccountAndLogin() {
        AccountResponse account = createAccount();
        urlSteps.login().queryParam("host", urlSteps.getPassportUrl() + "passport")
                .queryParam("mode", "auth")
                .queryParam("login", account.getLogin())
                .queryParam("passwd", account.getPassword()).open();
        urlSteps.testing().open();
    }

    @Step("Авторизуемся за единый аккаунт")
    public void commonAccountLogin() {
        urlSteps.login().queryParam("host", urlSteps.getPassportUrl() + "passport")
                .queryParam("mode", "auth")
                .queryParam("login", COMMON_LOGIN)
                .queryParam("passwd", COMMON_PASSWORD).open();
        urlSteps.testing().open();
    }

    @Step("Авторизуемся за аккаунт с офферами")
    public void accountWithOffersLogin() {
        urlSteps.login().queryParam("host", urlSteps.getPassportUrl() + "passport")
                .queryParam("mode", "auth")
                .queryParam("login", WITH_OFFERS_LOGIN)
                .queryParam("passwd", WITH_OFFERS_PASSWORD).open();
        urlSteps.testing().open();
    }

    @Step("Авторизуемся за аккаунт для публикации офферов")
    public void accountForOfferCreationLogin() {
        currentAccount = getRandomAccount();
        urlSteps.login().queryParam("host", urlSteps.getPassportUrl() + "passport")
                .queryParam("mode", "auth")
                .queryParam("login", currentAccount.getLogin())
                .queryParam("passwd", currentAccount.getPassword()).open();
        urlSteps.testing().open();
    }

    private AccountsForOfferCreation getRandomAccount() {
        return AccountsForOfferCreation.values()[new Random().nextInt(AccountsForOfferCreation.values().length)];
    }

    public PassportSteps() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);


        OkHttpClient.Builder httpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient().newBuilder();
        httpClient.addInterceptor(logging);
        httpClient.addInterceptor(new AllureOkHttp3());
        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Interceptor.Chain chain) throws IOException {
                Request original = chain.request();
                HttpUrl url = original.url().newBuilder()
                        .addQueryParameter("tus_consumer", config.getTusConsumer())
                        .addQueryParameter("env", config.getTusEnviroment()).build();

                Request newRequest = original.newBuilder()
                        .url(url)
                        .header("Authorization", "OAuth " + config.getTusToken())
                        .build();

                return chain.proceed(newRequest);
            }
        });

        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build());
    }

    private AccountResponse createAccount() {
        try {
            CreateAccountResponse response = retrofit.baseUrl(TUS_HOST).build()
                    .create(TusApiService.class)
                    .createAccount()
                    .execute().body();
            return response.getAccount();

        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать аккаунт", e);

        }
    }

    public String getCurrentAccountMail() {
        return format("%s@yandex.ru", currentAccount.getLogin());
    }

}
