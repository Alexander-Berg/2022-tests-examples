package ru.yandex.qe.hitman.testing.web;

import java.util.EventListener;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class TestingWebServer {

    public static final int DEFAULT_PORT = 24333;

    private final Server server;
    private final int port;
    private ServletContextHandler contextHandler;

    public TestingWebServer(String pathSpecification, HttpServlet servlet) {
        this(24333, pathSpecification, servlet);
    }

    TestingWebServer(int port, String pathSpecification, HttpServlet servlet) {
        this(port);
        this.addServlet(pathSpecification, servlet);
    }

    public TestingWebServer(int port) {
        this.port = port;
        this.contextHandler = new ServletContextHandler();
        this.contextHandler.setContextPath("/");
        this.server = new Server(port);
        this.server.setHandler(this.contextHandler);
    }

    public void addServlet(String pathSpecification, HttpServlet servlet) {
        ServletHolder servletHolder = new ServletHolder(servlet);
        this.contextHandler.addServlet(servletHolder, pathSpecification);
    }

    public void start() throws Exception {
        this.server.start();
    }

    public void stop() throws Exception {
        this.server.stop();
    }

    public int getPort() {
        return this.port;
    }

    public void startUpRunShutdown(Runnable runnable) throws Exception {
        try {
            this.start();
            runnable.run();
        } finally {
            try {
                this.stop();
            } catch (Exception e) {
                throw new AssertionError("fail to stop test web server", e);
            }

        }

    }

    public void addEventListener(EventListener eventListener) {
        this.contextHandler.addEventListener(eventListener);
    }

}
