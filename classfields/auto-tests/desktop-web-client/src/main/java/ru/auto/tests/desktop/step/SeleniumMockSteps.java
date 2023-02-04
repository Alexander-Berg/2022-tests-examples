package ru.auto.tests.desktop.step;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import io.qameta.allure.Step;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.Matcher;
import org.openqa.selenium.devtools.v101.fetch.Fetch;
import org.openqa.selenium.devtools.v101.network.Network;
import org.openqa.selenium.devtools.v101.network.model.Request;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.beans.LogEntity;
import ru.auto.tests.desktop.managers.DevToolsManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.desktop.utils.Utils.getResourceAsString;

public class SeleniumMockSteps extends BasePageSteps {

    @Inject
    private DevToolsManager devToolsManager;

    @Inject
    private DesktopConfig config;

    public void assertWithWaiting(Matcher<List<Request>> matcher) {
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(devToolsManager.getAllRequests(), matcher));
    }

    @Step("Получаем заголовок «x-autoru-app-id» для url «{url}»")
    public String getAutoruAppIdForUrl(String url) {
        List<LogEntry> logEntries = getDriver().manage().logs().get(LogType.PERFORMANCE).getAll();

        LogEntity.Message log = logEntries.stream()
                .map(logEntry -> new Gson().fromJson(logEntry.getMessage(), LogEntity.class))
                .map(LogEntity::getMessage)
                .filter(logMessage -> logMessage.getMethod().equals(Network.responseReceived().toString()))
                .filter(logMessage -> logMessage.getParams().getResponse().getUrl().equals(url))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(format("Can't find response with url %s", url)));

        return log.getParams().getResponse().getHeaders().getXAutoruAppId();
    }

    @Step("Добавляем мок браузерных запросов биллинг попапа нового траста")
    public void setNewTrustBillingBrowserMock() {
        devToolsManager.getDevTools().send(Fetch.enable(Optional.empty(), Optional.empty()));

        devToolsManager.getDevTools().addListener(Fetch.requestPaused(), req -> {
            if (req.getRequest().getUrl().contains("check_payment")) {
                devToolsManager.getDevTools().send(
                        Fetch.fulfillRequest(
                                req.getRequestId(),
                                200,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(getRawResponse("browserMocks/CheckPayment.json")),
                                Optional.empty())
                );
            } else if (req.getRequest().getUrl().contains("card_form")) {
                devToolsManager.getDevTools().send(
                        Fetch.fulfillRequest(
                                req.getRequestId(),
                                200,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(getRawResponse("browserMocks/CardFrom.txt")),
                                Optional.empty()));
            } else if (req.getRequest().getUrl().contains("update_payment")) {
                devToolsManager.getDevTools().send(
                        Fetch.fulfillRequest(
                                req.getRequestId(),
                                200,
                                Optional.empty(),
                                Optional.empty(),
                                Optional.of(getRawResponse("browserMocks/UpdatePayment.json")),
                                Optional.empty()));
            } else {
                devToolsManager.getDevTools().send(
                        Fetch.continueRequest(
                                req.getRequestId(),
                                Optional.of(req.getRequest().getUrl()),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()));
            }

        });
    }

    private String getRawResponse(String path) {
        String fullPath = format("mocksConfigurable/%s", path);
        String response;

        try {
            response = getResourceAsString(fullPath);
        } catch (NullPointerException e) {
            throw new RuntimeException(format("Can't read file '%s'", fullPath), e.getCause());
        }

        return Base64.encode(response.getBytes());
    }

    public static NameValuePair queryPair(String name, String value) {
        return new BasicNameValuePair(name, value);
    }

    public String formatGoal(String goalName) {
        return format("goal://%s/%s", config.getBaseDomain(), goalName);
    }

}
