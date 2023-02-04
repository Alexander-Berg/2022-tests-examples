package ru.yandex.general.rules;

import io.qameta.allure.Attachment;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import ru.auto.tests.commons.browsermob.ProxyServerManager;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.yandex.general.config.GeneralWebConfig;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class ProxyTimeoutResource extends ExternalResource {

    @Inject
    private ProxyServerManager proxyServerManager;

    @Inject
    private WebDriverManager driverManager;

    @Inject
    private GeneralWebConfig config;

    protected void before() throws Throwable {
        proxyServerManager.startServer();
        BrowserMobProxyServer proxyServer = proxyServerManager.getServer();
        proxyServer.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT,
                CaptureType.REQUEST_HEADERS, CaptureType.RESPONSE_HEADERS);
        Proxy proxy = ClientUtil.createSeleniumProxy(proxyServer);

        driverManager.updateCapabilities(capabilities -> {
            if (config.isLocalDebug()) {
                String proxyStr = "localhost:" + proxyServerManager.getServer().getPort();
                proxy.setHttpProxy(proxyStr);
                proxy.setSslProxy(proxyStr);
            }
            capabilities.setCapability(CapabilityType.PROXY, proxy);
        });
        proxyServer.newHar();
    }

    @Attachment(value = "HAR log", type = "text/html")
    private byte[] attachHarLog() {
        Har har = proxyServerManager.getServer().getHar();
        try (Writer writer = new StringWriter()) {
            har.writeTo(writer);
            return writer.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace(System.out);
            return new byte[]{};
        }
    }

    protected void after() {
        attachHarLog();
        proxyServerManager.stopServer();
    }

}
