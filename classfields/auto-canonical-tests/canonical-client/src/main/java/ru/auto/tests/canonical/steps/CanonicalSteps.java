package ru.auto.tests.canonical.steps;

import io.qameta.allure.Step;
import okhttp3.Response;
import ru.auto.tests.canonical.client.CanonicalClient;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CanonicalSteps {

    @Step("Делаем десктопный запрос {url} с кукой {cookie}")
    public Response makeDesktopRequest(String url, String cookie) throws IOException {
        return CanonicalClient.create().desktopCall(url, cookie);
    }

    @Step("Делаем мобайл запрос {url} с кукой {cookie}")
    public Response makeMobileRequest(String url, String cookie) throws IOException {
        return CanonicalClient.create().mobileCall(url, cookie);
    }

    @Step("Проверяем canonical")
    public void checkCanonical(String body, String canonical) {
        Matcher m = Pattern.compile("rel=\"canonical\" href=\"(.+?)\"/>").matcher(body);
        m.find();
        assertThat(m.group(1), equalTo(canonical));
    }

    @Step("Проверяем отсутствие meta тэга с robots noindex")
    public void notContainsRobotsNoindex(String body) {
        assertThat("Проверяем отсутствие «<meta name=\"robots\" content=\"noindex»",
                body.contains("<meta name=\"robots\" content=\"noindex"), is(false));
    }

}
