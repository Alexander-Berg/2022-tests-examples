package ru.auto.tests.commons.rule;

import lombok.Getter;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import ru.auto.tests.commons.browsermob.ProxyServerManager;
import ru.auto.tests.commons.webdriver.WebDriverManager;

import javax.inject.Inject;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class ProxyServerResource extends ExternalResource {

    @Inject
    @Getter
    private ProxyServerManager proxyServerManager;

    @Inject
    @Getter
    private WebDriverManager driverManager;

    protected void before() throws Throwable {
        getProxyServerManager().startServer();
        BrowserMobProxyServer proxyServer = getProxyServerManager().getServer();

        getDriverManager().updateCapabilities(capabilities -> {
            Proxy proxy = ClientUtil.createSeleniumProxy(proxyServer);
            capabilities.setCapability(CapabilityType.PROXY, proxy);
        });
    }

    protected void after() {
        getProxyServerManager().stopServer();
    }

}
