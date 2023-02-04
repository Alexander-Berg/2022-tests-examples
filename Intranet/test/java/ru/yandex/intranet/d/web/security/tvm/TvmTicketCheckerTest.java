package ru.yandex.intranet.d.web.security.tvm;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import ru.yandex.intranet.d.web.security.model.YaAuthenticationToken;
import ru.yandex.intranet.d.web.security.model.YaPrincipal;
import ru.yandex.intranet.d.web.security.tvm.model.InvalidServiceTicket;
import ru.yandex.intranet.d.web.security.tvm.model.InvalidUserTicket;
import ru.yandex.intranet.d.web.security.tvm.model.ValidServiceTicket;
import ru.yandex.intranet.d.web.security.tvm.model.ValidUserTicket;

/**
 * TVM ticket checker test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@ExtendWith(MockServerExtension.class)
public class TvmTicketCheckerTest {

    @Test
    public void testValidServiceTicket(MockServerClient client) throws JsonProcessingException {
        ValidServiceTicket ticket = new ValidServiceTicket(1L, 2L, null, "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
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
        TvmTicketChecker ticketChecker = new TvmTicketChecker(tvmClient, 2L, "quota-management:use");
        YaAuthenticationToken authToken = ticketChecker.checkService("serviceTicket").block();
        Assertions.assertNotNull(authToken);
        YaPrincipal principal = (YaPrincipal) authToken.getPrincipal();
        Assertions.assertNotNull(principal);
        Assertions.assertTrue(principal.getUid().isEmpty());
        Assertions.assertTrue(principal.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principal.getOAuthClientName().isEmpty());
        Assertions.assertTrue(principal.getTvmServiceId().isPresent());
        Assertions.assertEquals(1L, principal.getTvmServiceId().get());
        client.reset();
    }

    @Test
    public void testInvalidServiceTicket(MockServerClient client) throws JsonProcessingException {
        InvalidServiceTicket ticket = new InvalidServiceTicket("Forbidden", "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
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
        TvmTicketChecker ticketChecker = new TvmTicketChecker(tvmClient, 2L, "quota-management:use");
        YaAuthenticationToken authToken = ticketChecker.checkService("serviceTicket").block();
        Assertions.assertNull(authToken);
        client.reset();
    }

    @Test
    public void testServiceTicketRetries(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        TvmTicketChecker ticketChecker = new TvmTicketChecker(tvmClient, 2L, "quota-management:use");
        Assertions.assertThrows(IllegalStateException.class,
                () -> ticketChecker.checkService("serviceTicket").block(), "Retries exhausted: 1/1");
        client.reset();
    }

    @Test
    public void testValidUserTicket(MockServerClient client) throws JsonProcessingException {
        ValidServiceTicket serviceTicket = new ValidServiceTicket(1L, 2L, null,
                "debug", "log");
        ValidUserTicket userTicket = new ValidUserTicket(3L, List.of(3L, 4L), List.of("bb:sessionid"),
                "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ValidServiceTicket.class)
                        .writeValueAsString(serviceTicket))
        );
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
                        .writeValueAsString(userTicket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        TvmTicketChecker ticketChecker = new TvmTicketChecker(tvmClient, 2L, "quota-management:use");
        YaAuthenticationToken authToken = ticketChecker.checkUser("userTicket", "serviceTicket").block();
        Assertions.assertNotNull(authToken);
        YaPrincipal principal = (YaPrincipal) authToken.getPrincipal();
        Assertions.assertNotNull(principal);
        Assertions.assertTrue(principal.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principal.getOAuthClientName().isEmpty());
        Assertions.assertTrue(principal.getTvmServiceId().isPresent());
        Assertions.assertTrue(principal.getUid().isPresent());
        Assertions.assertEquals(1L, principal.getTvmServiceId().get());
        Assertions.assertEquals("3", principal.getUid().get());
        client.reset();
    }

    @Test
    public void testInvalidUserTicket(MockServerClient client) throws JsonProcessingException {
        ValidServiceTicket serviceTicket = new ValidServiceTicket(1L, 2L, null,
                "debug", "log");
        InvalidUserTicket userTicket = new InvalidUserTicket("Forbidden", "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ValidServiceTicket.class)
                        .writeValueAsString(serviceTicket))
        );
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
                        .writeValueAsString(userTicket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        TvmTicketChecker ticketChecker = new TvmTicketChecker(tvmClient, 2L, "quota-management:use");
        YaAuthenticationToken authToken = ticketChecker.checkUser("userTicket", "serviceTicket").block();
        Assertions.assertNull(authToken);
        client.reset();
    }

    @Test
    public void testValidServiceTicketCache(MockServerClient client) throws JsonProcessingException {
        ValidServiceTicket ticket = new ValidServiceTicket(1L, 2L, null, "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
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
        TvmTicketChecker ticketChecker = new TvmTicketChecker(tvmClient, 2L, "quota-management:use");
        YaAuthenticationToken authTokenFirst = ticketChecker.checkService("serviceTicket").block();
        Assertions.assertNotNull(authTokenFirst);
        YaPrincipal principalFirst = (YaPrincipal) authTokenFirst.getPrincipal();
        Assertions.assertNotNull(principalFirst);
        Assertions.assertTrue(principalFirst.getUid().isEmpty());
        Assertions.assertTrue(principalFirst.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principalFirst.getOAuthClientName().isEmpty());
        Assertions.assertTrue(principalFirst.getTvmServiceId().isPresent());
        Assertions.assertEquals(1L, principalFirst.getTvmServiceId().get());
        YaAuthenticationToken authTokenSecond = ticketChecker.checkService("serviceTicket").block();
        Assertions.assertNotNull(authTokenSecond);
        YaPrincipal principalSecond = (YaPrincipal) authTokenSecond.getPrincipal();
        Assertions.assertNotNull(principalSecond);
        Assertions.assertTrue(principalSecond.getUid().isEmpty());
        Assertions.assertTrue(principalSecond.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principalSecond.getOAuthClientName().isEmpty());
        Assertions.assertTrue(principalSecond.getTvmServiceId().isPresent());
        Assertions.assertEquals(1L, principalSecond.getTvmServiceId().get());
        client.reset();
    }

    @Test
    public void testValidUserTicketCache(MockServerClient client) throws JsonProcessingException {
        ValidServiceTicket serviceTicket = new ValidServiceTicket(1L, 2L, null,
                "debug", "log");
        ValidUserTicket userTicket = new ValidUserTicket(3L, List.of(3L, 4L), List.of("bb:sessionid"),
                "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ValidServiceTicket.class)
                        .writeValueAsString(serviceTicket))
        );
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
                        .writeValueAsString(userTicket))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        TvmTicketChecker ticketChecker = new TvmTicketChecker(tvmClient, 2L, "quota-management:use");
        YaAuthenticationToken authTokenFirst = ticketChecker.checkUser("userTicket", "serviceTicket").block();
        Assertions.assertNotNull(authTokenFirst);
        YaPrincipal principalFirst = (YaPrincipal) authTokenFirst.getPrincipal();
        Assertions.assertNotNull(principalFirst);
        Assertions.assertTrue(principalFirst.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principalFirst.getOAuthClientName().isEmpty());
        Assertions.assertTrue(principalFirst.getTvmServiceId().isPresent());
        Assertions.assertTrue(principalFirst.getUid().isPresent());
        Assertions.assertEquals(1L, principalFirst.getTvmServiceId().get());
        Assertions.assertEquals("3", principalFirst.getUid().get());
        YaAuthenticationToken authTokenSecond = ticketChecker.checkUser("userTicket", "serviceTicket").block();
        Assertions.assertNotNull(authTokenSecond);
        YaPrincipal principalSecond = (YaPrincipal) authTokenSecond.getPrincipal();
        Assertions.assertNotNull(principalSecond);
        Assertions.assertTrue(principalSecond.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principalSecond.getOAuthClientName().isEmpty());
        Assertions.assertTrue(principalSecond.getTvmServiceId().isPresent());
        Assertions.assertTrue(principalSecond.getUid().isPresent());
        Assertions.assertEquals(1L, principalSecond.getTvmServiceId().get());
        Assertions.assertEquals("3", principalSecond.getUid().get());
        client.reset();
    }

    @Test
    public void testValidServiceTicketErrorNotCached(MockServerClient client) throws JsonProcessingException {
        ValidServiceTicket ticket = new ValidServiceTicket(1L, 2L, null, "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        TvmTicketChecker ticketChecker = new TvmTicketChecker(tvmClient, 2L, "quota-management:use");
        Assertions.assertThrows(IllegalStateException.class,
                () -> ticketChecker.checkService("serviceTicket").block());
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
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
        YaAuthenticationToken authTokenSecond = ticketChecker.checkService("serviceTicket").block();
        Assertions.assertNotNull(authTokenSecond);
        YaPrincipal principalSecond = (YaPrincipal) authTokenSecond.getPrincipal();
        Assertions.assertNotNull(principalSecond);
        Assertions.assertTrue(principalSecond.getUid().isEmpty());
        Assertions.assertTrue(principalSecond.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principalSecond.getOAuthClientName().isEmpty());
        Assertions.assertTrue(principalSecond.getTvmServiceId().isPresent());
        Assertions.assertEquals(1L, principalSecond.getTvmServiceId().get());
        client.reset();
    }

    @Test
    public void testValidUserTicketErrorNotCached(MockServerClient client) throws JsonProcessingException {
        ValidServiceTicket serviceTicket = new ValidServiceTicket(1L, 2L, null,
                "debug", "log");
        ValidUserTicket userTicket = new ValidUserTicket(3L, List.of(3L, 4L), List.of("bb:sessionid"),
                "debug", "log");
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checksrv")
                        .withQueryStringParameter("dst", "2")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.unlimited()
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(ValidServiceTicket.class)
                        .writeValueAsString(serviceTicket))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/checkusr")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("X-Ya-User-Ticket", "userTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        TvmTicketChecker ticketChecker = new TvmTicketChecker(tvmClient, 2L, "quota-management:use");
        Assertions.assertThrows(IllegalStateException.class,
                () -> ticketChecker.checkUser("userTicket", "serviceTicket").block());
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
                        .writeValueAsString(userTicket))
        );
        YaAuthenticationToken authTokenSecond = ticketChecker.checkUser("userTicket", "serviceTicket").block();
        Assertions.assertNotNull(authTokenSecond);
        YaPrincipal principalSecond = (YaPrincipal) authTokenSecond.getPrincipal();
        Assertions.assertNotNull(principalSecond);
        Assertions.assertTrue(principalSecond.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principalSecond.getOAuthClientName().isEmpty());
        Assertions.assertTrue(principalSecond.getTvmServiceId().isPresent());
        Assertions.assertTrue(principalSecond.getUid().isPresent());
        Assertions.assertEquals(1L, principalSecond.getTvmServiceId().get());
        Assertions.assertEquals("3", principalSecond.getUid().get());
        client.reset();
    }

}
