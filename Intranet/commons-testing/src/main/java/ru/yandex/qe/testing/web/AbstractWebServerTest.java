package ru.yandex.qe.testing.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ru.yandex.qe.testing.web.TestingWebServer.DEFAULT_PORT;

/**
 * User: terry
 * Date: 30.08.13
 * Time: 22:42
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractWebServerTest {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected TestingWebServer webServer;

    protected int getPort() {
        return DEFAULT_PORT;
    }

    public void addServlet(String pathSpecification, HttpServlet servlet) {
        webServer.addServlet(pathSpecification, servlet);
    }

    public void addResponseServlet(String pathSpecification, final String response) {
        webServer.addServlet(pathSpecification, new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setCharacterEncoding("UTF-8");
                PrintWriter writer = resp.getWriter();
                writer.write(response);
                writer.close();
            }
        });
    }

    public String getHostPort() {
        return String.format("localhost:%s", getPort());
    }

    public String getHostUrl() {
        return String.format("http://localhost:%s", getPort());
    }

    public String resolveUrl(String path) {
        return String.format("http://localhost:%s/%s", getPort(), path);
    }

    @BeforeEach
    public void start() throws Exception {
        logger.info("Starting server on port {}...", DEFAULT_PORT);
        webServer = new TestingWebServer(DEFAULT_PORT);
        webServer.start();
    }

    @AfterEach
    public void stop() throws Exception {
        webServer.stop();
        logger.info("Stopped server on port {}", DEFAULT_PORT);
    }
}
