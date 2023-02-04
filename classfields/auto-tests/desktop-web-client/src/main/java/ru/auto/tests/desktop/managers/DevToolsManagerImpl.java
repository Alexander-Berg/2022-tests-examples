package ru.auto.tests.desktop.managers;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v101.network.Network;
import org.openqa.selenium.devtools.v101.network.model.Request;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.desktop.DesktopConfig;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.String.format;

public class DevToolsManagerImpl implements DevToolsManager {

    private DevTools devTools;
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    @Inject
    private DesktopConfig config;

    @Inject
    private WebDriverManager webDriverManager;

    @Override
    public void startDevTools() {
        String webdriverRemoteUrl = config.getWebdriverRemoteUrl();

        if (webdriverRemoteUrl == null) {
            startLocalDevTools();
        } else {
            startRemoteDevTools(webdriverRemoteUrl);
        }
    }

    @Override
    public void startRecordRequests() {
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

        devTools.addListener(Network.requestWillBeSent(), request -> requests.add(request.getRequest()));
    }

    @Override
    public void stopRecordRequests() {
        devTools.clearListeners();
        requests.clear();
    }

    @Override
    public DevTools getDevTools() {
        if (devTools == null) {
            throw new IllegalStateException("DevTools не были проинициализированы");
        }
        return devTools;
    }

    @Override
    public List<Request> getAllRequests() {
        return requests;
    }

    private void startRemoteDevTools(String webdriverRemoteUrl) {
        RemoteWebDriver rawDriver = (RemoteWebDriver) webDriverManager.getDriver();
        String sessionId = ((RemoteWebDriver) webDriverManager.getDriver()).getSessionId().toString();

        URL selenoidUrl;
        try {
            selenoidUrl = new URL(webdriverRemoteUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(format("Can't create URL class from value «%s»", webdriverRemoteUrl));
        }

        try {
            Field capabilitiesField = RemoteWebDriver.class.getDeclaredField("capabilities");
            capabilitiesField.setAccessible(true);

            String devtoolsUrl = String.format("ws://%s:%s/devtools/%s", selenoidUrl.getHost(), selenoidUrl.getPort(), sessionId);

            MutableCapabilities mutableCapabilities = (MutableCapabilities) capabilitiesField.get(rawDriver);
            mutableCapabilities.setCapability("se:cdp", devtoolsUrl);
            mutableCapabilities.setCapability("se:cdpVersion", mutableCapabilities.getBrowserVersion());
        } catch (Exception e) {
            throw new RuntimeException("Failed to spoof RemoteWebDriver capabilities", e.getCause());
        }

        RemoteWebDriver augmentedDriver = (RemoteWebDriver) new Augmenter().augment(rawDriver);
        devTools = ((HasDevTools) augmentedDriver).getDevTools();

        devTools.createSessionIfThereIsNotOne();
    }

    private void startLocalDevTools() {
        devTools = ((HasDevTools) webDriverManager.getDriver()).getDevTools();

        devTools.createSessionIfThereIsNotOne();
    }

    /*public void listenResponses(String requestUrl) {
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

        devTools.addListener(Network.responseReceived(), requestSent -> {

            if (requestSent.getResponse().getUrl().contains(requestUrl)) {
                System.out.println("Status => " + requestSent.getResponse().getStatus());
                System.out.println("Status text => " + requestSent.getResponse().getStatusText());

                Command<Network.GetResponseBodyResponse> getBody = Network.getResponseBody(requestSent.getRequestId());
                Network.GetResponseBodyResponse response = devTools.send(getBody);
                System.out.println(response.getBody());
                System.out.println("------------------------------------------------------");
            }

        });
    }*/
}
