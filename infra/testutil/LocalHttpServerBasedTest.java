package ru.yandex.infra.controller.testutil;

import java.util.Map;

import javax.servlet.http.HttpServlet;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.misc.ip.IpPortUtils;

public abstract class LocalHttpServerBasedTest {
    private Server server;
    private int port;
    private DefaultAsyncHttpClient client;

    @BeforeEach
    void before() throws Exception {
        port = IpPortUtils.getFreeLocalPort().getPort();
        int workerThreads = 4;
        server = new Server(new QueuedThreadPool(workerThreads, workerThreads));

        int selectorThreads = 2;
        ServerConnector connector = new ServerConnector(server, 0, selectorThreads);
        connector.setReuseAddress(true);
        connector.setHost("::");
        connector.setPort(port);
        connector.setIdleTimeout(4000);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        getServlets().forEach((path, servlet) -> context.addServlet(new ServletHolder(servlet), path));

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{context, new DefaultHandler()});
        server.setHandler(handlers);
        server.start();

        client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().build());
    }

    @AfterEach
    void after() throws Exception {
        if (server != null) {
            server.stop();
            server.join();
        }
        if (client != null) {
            client.close();
        }
    }

    protected String getUrl() {
        return String.format("http://localhost:%d", port);
    }

    protected DefaultAsyncHttpClient getClient() {
        return client;
    }

    protected abstract Map<String, HttpServlet> getServlets();
}
