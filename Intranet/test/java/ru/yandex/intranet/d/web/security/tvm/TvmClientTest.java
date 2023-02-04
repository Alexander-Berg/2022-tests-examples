package ru.yandex.intranet.d.web.security.tvm;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import ru.yandex.intranet.d.web.security.tvm.model.CheckedServiceTicket;
import ru.yandex.intranet.d.web.security.tvm.model.CheckedUserTicket;
import ru.yandex.intranet.d.web.security.tvm.model.InvalidServiceTicket;
import ru.yandex.intranet.d.web.security.tvm.model.InvalidUserTicket;
import ru.yandex.intranet.d.web.security.tvm.model.TvmEnvironment;
import ru.yandex.intranet.d.web.security.tvm.model.TvmStatus;
import ru.yandex.intranet.d.web.security.tvm.model.TvmTicket;
import ru.yandex.intranet.d.web.security.tvm.model.ValidServiceTicket;
import ru.yandex.intranet.d.web.security.tvm.model.ValidUserTicket;

/**
 * TVM client test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@ExtendWith(MockServerExtension.class)
public class TvmClientTest {

    @Test
    public void testPingOk(MockServerClient client) {
        client.when(HttpRequest.request()
                .withMethod("GET")
                .withPath("/tvm/ping")
                .withHeader("Authorization", "authtoken")
                .withHeader("Accept", MediaType.TEXT_PLAIN.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("OK")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(new TvmStatus(TvmStatus.Status.OK, "OK"), tvmClient.ping().block());
        client.reset();
    }

    @Test
    public void testPingWarn(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/ping")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.TEXT_PLAIN.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(206)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Warning")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(new TvmStatus(TvmStatus.Status.WARN, "Warning"), tvmClient.ping().block());
        client.reset();
    }

    @Test
    public void testPingError(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/ping")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.TEXT_PLAIN.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(new TvmStatus(TvmStatus.Status.ERROR, "Error"), tvmClient.ping().block());
        client.reset();
    }

    @Test
    public void testPingUnexpected(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/ping")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.TEXT_PLAIN.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(401)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Invalid authentication token")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertThrows(WebClientResponseException.Unauthorized.class, () -> tvmClient.ping().block());
        client.reset();
    }

    @Test
    public void testTicketsOk(MockServerClient client) throws JsonProcessingException {
        Map<String, TvmTicket> expectedResponse = Map.of(
                "testGood", new TvmTicket("testTicket", 1L, null),
                "testBad", new TvmTicket(null, 2L, "Error"));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "source")
                        .withQueryStringParameter("dsts", "dstOne,dstTwo")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(expectedResponse,
                tvmClient.tickets("source", List.of("dstOne", "dstTwo")).block());
        client.reset();
    }

    @Test
    public void testTicketsFailure(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "source")
                        .withQueryStringParameter("dsts", "dstOne,dstTwo")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(401)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Invalid authentication token")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertThrows(WebClientResponseException.Unauthorized.class,
                () -> tvmClient.tickets("source", List.of("dstOne", "dstTwo")).block());
        client.reset();
    }

    @Test
    public void testTicketsNoSrcOk(MockServerClient client) throws JsonProcessingException {
        Map<String, TvmTicket> expectedResponse = Map.of(
                "testGood", new TvmTicket("testTicket", 1L, null),
                "testBad", new TvmTicket(null, 2L, "Error"));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("dsts", "dstOne,dstTwo")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(expectedResponse,
                tvmClient.tickets(List.of("dstOne", "dstTwo")).block());
        client.reset();
    }

    @Test
    public void testTicketsNoSrcFailure(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("dsts", "dstOne,dstTwo")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(401)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Invalid authentication token")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertThrows(WebClientResponseException.Unauthorized.class,
                () -> tvmClient.tickets(List.of("dstOne", "dstTwo")).block());
        client.reset();
    }

    @Test
    public void testKeysOk(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/keys")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.TEXT_PLAIN.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("KEYS")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals("KEYS", tvmClient.keys().block());
        client.reset();
    }

    @Test
    public void testKeysFailure(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/keys")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.TEXT_PLAIN.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(401)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Invalid authentication token")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertThrows(WebClientResponseException.Unauthorized.class, () -> tvmClient.keys().block());
        client.reset();
    }

    @Test
    public void testCheckServiceTicketOk(MockServerClient client) throws JsonProcessingException {
        ValidServiceTicket ticket = new ValidServiceTicket(1L, 2L, null, "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "destination")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ValidServiceTicket.class)
                        .writeValueAsString(ticket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(CheckedServiceTicket.valid(ticket),
                tvmClient.checkServiceTicket("destination", "serviceTicket").block());
        client.reset();
    }

    @Test
    public void testCheckServiceTicketNoDstOk(MockServerClient client) throws JsonProcessingException {
        ValidServiceTicket ticket = new ValidServiceTicket(1L, 2L, null, "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ValidServiceTicket.class)
                        .writeValueAsString(ticket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(CheckedServiceTicket.valid(ticket),
                tvmClient.checkServiceTicket("serviceTicket").block());
        client.reset();
    }

    @Test
    public void testCheckServiceTicketForbidden(MockServerClient client) throws JsonProcessingException {
        InvalidServiceTicket ticket = new InvalidServiceTicket("Forbidden", "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "destination")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(403)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(InvalidServiceTicket.class)
                        .writeValueAsString(ticket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(CheckedServiceTicket.invalid(ticket),
                tvmClient.checkServiceTicket("destination", "serviceTicket").block());
        client.reset();
    }

    @Test
    public void testCheckServiceTicketNoDstForbidden(MockServerClient client) throws JsonProcessingException {
        InvalidServiceTicket ticket = new InvalidServiceTicket("Forbidden", "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(403)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(InvalidServiceTicket.class)
                        .writeValueAsString(ticket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(CheckedServiceTicket.invalid(ticket),
                tvmClient.checkServiceTicket("serviceTicket").block());
        client.reset();
    }

    @Test
    public void testCheckServiceTicketFailure(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "destination")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(401)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Invalid authentication token")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertThrows(WebClientResponseException.Unauthorized.class,
                () -> tvmClient.checkServiceTicket("destination", "serviceTicket").block());
        client.reset();
    }

    @Test
    public void testCheckServiceTicketNoDstFailure(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(401)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Invalid authentication token")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertThrows(WebClientResponseException.Unauthorized.class,
                () -> tvmClient.checkServiceTicket("serviceTicket").block());
        client.reset();
    }

    @Test
    public void testCheckUserTicketOk(MockServerClient client) throws JsonProcessingException {
        ValidUserTicket ticket = new ValidUserTicket(1L, List.of(1L, 2L), List.of("bb:sessionid"), "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checkusr")
                        .withQueryStringParameter("override_env", "prod")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-User-Ticket", "userTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ValidUserTicket.class)
                        .writeValueAsString(ticket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(CheckedUserTicket.valid(ticket),
                tvmClient.checkUserTicket(TvmEnvironment.PRODUCTION, "userTicket").block());
        client.reset();
    }

    @Test
    public void testCheckUserTicketNoEnvOk(MockServerClient client) throws JsonProcessingException {
        ValidUserTicket ticket = new ValidUserTicket(1L, List.of(1L, 2L), List.of("bb:sessionid"), "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checkusr")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-User-Ticket", "userTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ValidUserTicket.class)
                        .writeValueAsString(ticket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(CheckedUserTicket.valid(ticket),
                tvmClient.checkUserTicket("userTicket").block());
        client.reset();
    }

    @Test
    public void testCheckUserTicketForbidden(MockServerClient client) throws JsonProcessingException {
        InvalidUserTicket ticket = new InvalidUserTicket("Forbidden", "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checkusr")
                        .withQueryStringParameter("override_env", "prod")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-User-Ticket", "userTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(403)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(InvalidUserTicket.class)
                        .writeValueAsString(ticket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(CheckedUserTicket.invalid(ticket),
                tvmClient.checkUserTicket(TvmEnvironment.PRODUCTION, "userTicket").block());
        client.reset();
    }

    @Test
    public void testCheckUserTicketNoEnvForbidden(MockServerClient client) throws JsonProcessingException {
        InvalidUserTicket ticket = new InvalidUserTicket("Forbidden", "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checkusr")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-User-Ticket", "userTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(403)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(InvalidUserTicket.class)
                        .writeValueAsString(ticket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertEquals(CheckedUserTicket.invalid(ticket),
                tvmClient.checkUserTicket("userTicket").block());
        client.reset();
    }

    @Test
    public void testCheckUserTicketFailure(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checkusr")
                        .withQueryStringParameter("override_env", "prod")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-User-Ticket", "userTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(401)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Invalid authentication token")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertThrows(WebClientResponseException.Unauthorized.class,
                () -> tvmClient.checkUserTicket(TvmEnvironment.PRODUCTION, "userTicket").block());
        client.reset();
    }

    @Test
    public void testCheckUserTicketNoEnvFailure(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checkusr")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-User-Ticket", "userTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(401)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Invalid authentication token")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        Assertions.assertThrows(WebClientResponseException.Unauthorized.class,
                () -> tvmClient.checkUserTicket("userTicket").block());
        client.reset();
    }

}
