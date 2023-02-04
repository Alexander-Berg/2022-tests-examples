package ru.auto.tests.desktop.step;

import io.qameta.allure.Step;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.api.TusApiService;
import ru.auto.tests.desktop.api.UnsafeOkHttpClient;
import ru.auto.tests.desktop.beans.tus.AccountResponse;
import ru.auto.tests.desktop.beans.tus.CreateAccountResponse;

import javax.inject.Inject;
import java.io.IOException;

import static java.lang.String.format;

public class YandexPassportSteps {

    private static final String TUS_HOST = "https://tus.yandex-team.ru/";
    private static final String AQUA_HOST = "http://aqua.yandex-team.ru/auth.html";
    private static final String PASSPORT_HOST = "https://passport.yandex.ru/passport";

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public DesktopConfig config;

    private final Retrofit.Builder retrofit;

    @Step("Создаем аккаунт и авторизуемся")
    public void createAccountAndLogin() {
        AccountResponse account = createAccount();

        loginWithAqua(account.getLogin(), account.getPassword());
        urlSteps.testing().open();
    }

    @Step("Авторизуемся за аккаунт с привязанной картой")
    public void loginAccounWithTiedCard() {
        loginWithAqua("yandex-team-39480.76031", "jSyf.PuZu");
        urlSteps.testing().open();
    }

    private void loginWithAqua(String login, String password) {
        urlSteps.fromUri(AQUA_HOST)
                .addParam("host", PASSPORT_HOST)
                .addParam("mode", "auth")
                .addParam("login", login)
                .addParam("passwd", password).open();
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

    public YandexPassportSteps() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);


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
                        .header("Authorization", format("OAuth %s", config.getTusToken()))
                        .build();

                return chain.proceed(newRequest);
            }
        });

        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build());
    }

}
