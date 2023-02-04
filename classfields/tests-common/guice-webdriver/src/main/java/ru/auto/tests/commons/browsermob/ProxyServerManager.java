package ru.auto.tests.commons.browsermob;

import net.lightbody.bmp.BrowserMobProxyServer;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public interface ProxyServerManager {

    void startServer() throws Throwable;

    void stopServer();

    BrowserMobProxyServer getServer();

}
