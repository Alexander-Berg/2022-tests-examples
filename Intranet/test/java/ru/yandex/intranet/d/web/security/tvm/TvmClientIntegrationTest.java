package ru.yandex.intranet.d.web.security.tvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.web.security.tvm.model.TvmStatus;
import ru.yandex.intranet.d.web.security.tvm.model.TvmTicket;

/**
 * TVM client integration test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
public class TvmClientIntegrationTest {

    @Test
    public void testPing() throws IOException {
        Path portPath = Path.of("tvmtool.port");
        Path tokenPath = Path.of("tvmtool.authtoken");
        if (!Files.exists(portPath) || !Files.exists(tokenPath)) {
            return;
        }
        try (Stream<String> portLines = Files.lines(portPath);
             Stream<String> tokenLines = Files.lines(tokenPath)) {
            int port = Integer.parseInt(portLines.findFirst().orElse(""));
            String token = tokenLines.findFirst().orElse("");
            TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                    "d", new TvmClientParams("http://localhost:" + port, token));
            Assertions.assertEquals(new TvmStatus(TvmStatus.Status.OK, "OK"), tvmClient.ping().block());
        }
    }

    @Test
    public void testKeys() throws IOException {
        Path portPath = Path.of("tvmtool.port");
        Path tokenPath = Path.of("tvmtool.authtoken");
        if (!Files.exists(portPath) || !Files.exists(tokenPath)) {
            return;
        }
        try (Stream<String> portLines = Files.lines(portPath);
             Stream<String> tokenLines = Files.lines(tokenPath)) {
            int port = Integer.parseInt(portLines.findFirst().orElse(""));
            String token = tokenLines.findFirst().orElse("");
            TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                    "d", new TvmClientParams("http://localhost:" + port, token));
            String keys = tvmClient.keys().block();
            Assertions.assertNotNull(keys);
            Assertions.assertFalse(keys.isEmpty());
        }
    }

    @Test
    public void testTickets() throws IOException {
        Path portPath = Path.of("tvmtool.port");
        Path tokenPath = Path.of("tvmtool.authtoken");
        if (!Files.exists(portPath) || !Files.exists(tokenPath)) {
            return;
        }
        try (Stream<String> portLines = Files.lines(portPath);
             Stream<String> tokenLines = Files.lines(tokenPath)) {
            int port = Integer.parseInt(portLines.findFirst().orElse(""));
            String token = tokenLines.findFirst().orElse("");
            TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                    "d", new TvmClientParams("http://localhost:" + port, token));
            Map<String, TvmTicket> tickets = tvmClient.tickets("42", List.of("100500", "100501")).block();
            Assertions.assertNotNull(tickets);
            Assertions.assertTrue(tickets.containsKey("he"));
            Assertions.assertTrue(tickets.containsKey("she"));
            Assertions.assertTrue(tickets.get("he").getTicket().isPresent());
            Assertions.assertTrue(tickets.get("she").getTicket().isPresent());
        }
    }

}
