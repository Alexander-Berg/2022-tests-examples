package ru.auto.tests.commons.browsermob;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.filters.HarCaptureFilter;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class DefaultProxyServerManager implements ProxyServerManager {

    private BrowserMobProxyServer server;

    @Override
    public void startServer() throws Throwable {
        LogManager.getLogger(HarCaptureFilter.class).setLevel(Level.FATAL);

        server = new BrowserMobProxyServer();
        server.setUseEcc(true);
        server.setTrustAllServers(true);
        server.start();
    }

    @Override
    public void stopServer() {
        server.stop();
    }

    @Override
    public BrowserMobProxyServer getServer() {
        return server;
    }
}
