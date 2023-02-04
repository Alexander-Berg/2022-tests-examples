package ru.auto.tests.redirect.steps;

import io.qameta.allure.Step;
import okhttp3.Response;
import ru.auto.tests.redirect.client.RedirectClient;

import java.io.IOException;

public class RedirectSteps {

    @Step("Делаем десктопный запрос с редиректом {url}")
    public Response makeDesktopRequest(String url) throws IOException {
        return RedirectClient.create().desktopCall(url);
    }

    @Step("Делаем десктопный запрос {url} с кукой {cookie}")
    public Response makeDesktopRequest(String url, String cookie) throws IOException {
        return RedirectClient.create().desktopCall(url, cookie);
    }

    @Step("Делаем десктопный запрос {url} с кукой {cookie} и user-agent {userAgent}")
    public Response makeDesktopRequest(String url, String cookie, String userAgent) throws IOException {
        return RedirectClient.create().desktopCall(url, cookie, userAgent);
    }

    @Step("Делаем мобайл запрос с редиректом {url}")
    public Response makeMobileRequestWithFollow(String url) throws IOException {
        return RedirectClient.create().mobileCall(url);
    }

    @Step("Делаем мобайл запрос без редиректа {url}")
    public Response makeMobileRequestWithoutFollow(String url) throws IOException {
        return RedirectClient.create().followRedirect(false).mobileCall(url);
    }

    @Step("Делаем мобайл запрос {url} с кукой {cookie} и user-agent {userAgent}")
    public Response makeMobileRequest(String url, String cookie, String userAgent) throws IOException {
        return RedirectClient.create().mobileCall(url, cookie, userAgent);
    }

}
