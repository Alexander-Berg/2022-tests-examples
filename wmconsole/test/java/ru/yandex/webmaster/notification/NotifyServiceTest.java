package ru.yandex.webmaster.notification;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: azakharov
 * Date: 04.04.14
 * Time: 19:49
 */
public class NotifyServiceTest {

    private NotifyService notifyService;
    private HttpServer server;

    @Before
    public void setUp() throws Exception {
        notifyService = new NotifyService();
        notifyService.setUrl("http://localhost:33333/notify");
        server = HttpServer.create(new InetSocketAddress(33333), 0);
    }

    @After
    public void tearDown() throws Exception {
        server.stop(3);
    }

    @Test
    public void testSuccessfulNotification() {
        String response =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<response servant=\"wmcnotifier\" version=\"0\" host=\"webmaster.dev.search.yandex.net\" actions=\"[notify]\"  executing-time=\"[106]\" ></response>";
        server.createContext("/notify", new ConstStringHttpHandler(response));
        server.setExecutor(null);
        server.start();

        assertTrue("successful notification", notifyService.makeNotification("wmtest.people.yandex.net", "blabla", "header"));
    }

    @Test
    public void testFailedNotification() {
        String response =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<response servant=\"wmcnotifier\" version=\"0\" host=\"webmaster.dev.search.yandex.net\" actions=\"[notify]\"  executing-time=\"[106]\" >\n"+
                        "<errors><error code=\"INTERNAL_EXCEPTION\"></error></errors>\n"+
                        "</response>";
        server.createContext("/notify", new ConstStringHttpHandler(response));
        server.setExecutor(null);
        server.start();

        assertFalse("failed notification", notifyService.makeNotification("", "", ""));
    }

    public static class ConstStringHttpHandler implements HttpHandler {
        private String response;

        public ConstStringHttpHandler(String response) {
            this.response = response;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
