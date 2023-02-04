package ru.yandex.qe.testing.web;

import java.util.EventListener;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * User: terry Date: 25.06.13 Time: 21:31
 */
public class TestingWebServer {
    public static final int DEFAULT_PORT = 24333;

    private final Server server;
    private final int port;
    private ServletContextHandler contextHandler;


    public TestingWebServer(String pathSpecification, HttpServlet servlet) {
        this(DEFAULT_PORT, pathSpecification, servlet);
    }

    TestingWebServer(int port, String pathSpecification, HttpServlet servlet) {
        this(port);
        addServlet(pathSpecification, servlet);
    }

    public TestingWebServer(int port) {
        this.port = port;
        contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
        server = new Server(port);
        server.setHandler(contextHandler);
    }

    public void addServlet(String pathSpecification, HttpServlet servlet) {
        final ServletHolder servletHolder = new ServletHolder(servlet);
        contextHandler.addServlet(servletHolder, pathSpecification);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public int getPort() {
        return port;
    }

    public void startUpRunShutdown(Runnable runnable) throws Exception {
        try {
            start();
            runnable.run();
        } finally {
            try {
                stop();
            } catch (Exception e) {
                fail("fail to stop test web server", e);
            }
        }
    }

    public void addEventListener(EventListener eventListener) {
        contextHandler.addEventListener(eventListener);
    }
}
