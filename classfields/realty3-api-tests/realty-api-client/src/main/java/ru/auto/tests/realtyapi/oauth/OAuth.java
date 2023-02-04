package ru.auto.tests.realtyapi.oauth;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import lombok.extern.log4j.Log4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realtyapi.config.RealtyApiConfig;
import ru.auto.tests.realtyapi.module.BlackboxModule.BlackBoxApiService;
import ru.auto.tests.realtyapi.module.BlackboxModule.BlackboxResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.apiAwait;

@Log4j
public class OAuth {

    @Inject
    private RealtyApiConfig apiConfig;

    @Inject
    private BlackBoxApiService blackBoxApiService;

    private static Map<String, String> tokens = new ConcurrentHashMap<>();

    private static final String CLIENT_ID = "421c67cdcea4454d898cec68b449fbd6";
    private static final String CLIENT_SECRET = "da2959fa1e0e440ba57d0b9b73d64460";
    private static final String GRANT_TYPE = "password";

    private Retrofit.Builder retrofit;

    public OAuth() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);
        httpClient.addInterceptor(new AllureOkHttp3());

        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build());
    }

    private static String hostAddr() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    @Step("OAuth «{account.login}»")
    public String getToken(Account account) {

        String oauthTokenUri = apiConfig.getOAuthTokenURI().toString();
        String header = CLIENT_ID + ":" + CLIENT_SECRET;
        String auth = "Basic " + Base64.getEncoder().encodeToString(header.getBytes());

        String token = apiAwait().atMost(10, SECONDS).until(
                () -> tokens.computeIfAbsent(
                        account.getLogin(),
                        key -> token(oauthTokenUri, auth, account)
                ),
                notNullValue()
        );

        apiAwait().atMost(5, SECONDS).pollInterval(1, SECONDS).until(() -> checkToken(token, account));

        return "OAuth " + token;
    }

    private String token(String oauthTokenUri, String auth, Account account) {
        try {
            Response<TokenResponse> response = retrofit.baseUrl(oauthTokenUri).build()
                    .create(OAuthService.class)
                    .token(auth, hostAddr(), GRANT_TYPE, account.getLogin(), account.getPassword(),
                            CLIENT_ID, CLIENT_SECRET)
                    .execute();

            if (response.code() != 200) {
                log.error(response.errorBody().string());
            }

            return response.body().getAccessToken();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to get oAuth token from %s", oauthTokenUri), e.getCause());
        }
    }

    private boolean checkToken(String token, Account account) {
        try {
            Response<BlackboxResponse> response = blackBoxApiService.checkOauthToken(token, hostAddr()).execute();
            return response.code() == 200 && response.body() != null && "OK".equals(response.body().error);
        } catch (IOException e) {
            throw new RuntimeException("Unable to validate oAuth token", e);
        }
    }

}
