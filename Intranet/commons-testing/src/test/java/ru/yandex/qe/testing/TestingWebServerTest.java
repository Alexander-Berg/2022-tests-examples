package ru.yandex.qe.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.testing.web.TestingWebServer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 25.06.13
 * Time: 21:39
 */
public class TestingWebServerTest {

    @Test
    public void check_ok() throws Exception {
        final String testContent = "check";
        final TestingWebServer testingWebServer = new TestingWebServer("/test", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getWriter().println(testContent);
                resp.getWriter().close();
            }
        });
        testingWebServer.startUpRunShutdown(new Runnable() {
            @Override
            public void run() {
                try {
                    final URL url = new URL(String.format("http://localhost:%s/test", TestingWebServer.DEFAULT_PORT));
                    final URLConnection urlConnection = url.openConnection();
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    final String remoteContent = reader.readLine();
                    reader.close();
                    assertThat(remoteContent, equalTo(testContent));
                } catch (Exception ex) {
                    Assertions.fail();
                }
            }
        });
    }
}
