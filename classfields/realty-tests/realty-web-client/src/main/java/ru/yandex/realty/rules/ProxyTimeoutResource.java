package ru.yandex.realty.rules;

import io.qameta.allure.Attachment;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import ru.auto.tests.commons.browsermob.ProxyServerManager;
import ru.auto.tests.commons.webdriver.WebDriverConfig;
import ru.auto.tests.commons.webdriver.WebDriverManager;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class ProxyTimeoutResource extends ExternalResource {

    @Inject
    private WebDriverConfig config;

    @Inject
    private ProxyServerManager proxyServerManager;

    @Inject
    private WebDriverManager driverManager;

    protected void before() throws Throwable {
        proxyServerManager.startServer();
        BrowserMobProxyServer proxyServer = proxyServerManager.getServer();

        driverManager.updateCapabilities(capabilities -> {
            Proxy proxy = ClientUtil.createSeleniumProxy(proxyServer);
            if (config.isLocal()) {
                String proxyStr = "localhost:"+ proxyServerManager.getServer().getPort();
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

