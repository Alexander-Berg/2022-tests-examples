package ru.auto.tests.redirect.client;

import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.aeonbits.owner.ConfigFactory;
import ru.auto.tests.redirect.RedirectConfig;

import java.io.IOException;

import static java.lang.String.format;

public class RedirectClient {

    RedirectConfig config = ConfigFactory.create(RedirectConfig.class, System.getProperties(), System.getenv());

    private OkHttpClient.Builder clientBuilder;

    private RedirectClient() {
        clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new AllureOkHttp3());
    }

    public static RedirectClient create() {
        return new RedirectClient();
    }

    public RedirectClient followRedirect(boolean follow) {
        clientBuilder.followRedirects(follow)
                .followSslRedirects(follow);

        return this;
    }

    public Response desktopCall(String url) throws IOException {
        Request request = new Request.Builder()
                .header("Cookie", format("exp_flags=without_exp; %s=%s;",
                        config.getBranchCookieName(), config.getBranchCookieValue()))
                .url(url)
                .build();

        return clientBuilder.build().newCall(request).execute();
    }

    public Response desktopCall(String url, String cookie) throws IOException {
        Request request = new Request.Builder()
                .header("Cookie", format("exp_flags=without_exp; %s=%s; %s",
                        config.getBranchCookieName(), config.getBranchCookieValue(), cookie))
                .url(url)
                .build();

        return clientBuilder.build().newCall(request).execute();
    }

    public Response desktopCall(String url, String cookie, String userAgent) throws IOException {
        Request request = new Request.Builder()
                .header("User-Agent", userAgent)
                .header("Cookie", format("exp_flags=without_exp; %s=%s; %s",
                        config.getBranchCookieName(), config.getBranchCookieValue(), cookie))
                .url(url)
                .build();

        return clientBuilder.build().newCall(request).execute();
    }

    public Response mobileCall(String url) throws IOException {
        Request request = new Request.Builder()
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1")
                .header("Cookie", format("gids=213; exp_flags=without_exp; %s=%s",
                        config.getBranchCookieName(), config.getBranchCookieValue()))
                .url(url)
                .build();

        return clientBuilder.build().newCall(request).execute();
    }

    public Response mobileCall(String url, String cookie, String userAgent) throws IOException {
        Request request = new Request.Builder()
                .header("User-Agent", userAgent)
                .header("Cookie", format("exp_flags=without_exp; %s=%s; %s",
                        config.getBranchCookieName(), config.getBranchCookieValue(), cookie))
                .url(url)
                .build();

        return clientBuilder.build().newCall(request).execute();
    }

}
