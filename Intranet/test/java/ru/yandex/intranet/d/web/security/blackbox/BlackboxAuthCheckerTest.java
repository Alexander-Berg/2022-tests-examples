package ru.yandex.intranet.d.web.security.blackbox;

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

import ru.yandex.intranet.d.web.security.blackbox.model.BlackboxOAuthResponse;
import ru.yandex.intranet.d.web.security.blackbox.model.BlackboxSessionIdResponse;
import ru.yandex.intranet.d.web.security.model.YaAuthenticationToken;
import ru.yandex.intranet.d.web.security.model.YaPrincipal;
import ru.yandex.intranet.d.web.security.tvm.TvmClient;
import ru.yandex.intranet.d.web.security.tvm.TvmClientParams;
import ru.yandex.intranet.d.web.security.tvm.model.TvmTicket;

/**
 * Blackbox authentication checker.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@ExtendWith(MockServerExtension.class)
public class BlackboxAuthCheckerTest {

    @Test
    public void testSessionIdSecureOk(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedBlackboxResponse = new BlackboxSessionIdResponse("OK", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.VALID, "VALID"),
                new BlackboxSessionIdResponse.Uid("1"), new BlackboxSessionIdResponse.Auth(true));
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        YaAuthenticationToken authToken = authChecker.checkSessionId("sessionId", "sslSessionId", "userIp").block();
        Assertions.assertNotNull(authToken);
        YaPrincipal principal = (YaPrincipal) authToken.getPrincipal();
        Assertions.assertNotNull(principal);
        Assertions.assertFalse(principal.getUid().isEmpty());
        Assertions.assertTrue(principal.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principal.getOAuthClientName().isEmpty());
        Assertions.assertFalse(principal.getTvmServiceId().isPresent());
        Assertions.assertEquals("1", principal.getUid().get());
        client.reset();
    }

    @Test
    public void testSessionIdNotSecure(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedBlackboxResponse = new BlackboxSessionIdResponse("OK", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.VALID, "VALID"),
                new BlackboxSessionIdResponse.Uid("1"), new BlackboxSessionIdResponse.Auth(false));
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        YaAuthenticationToken authToken = authChecker.checkSessionId("sessionId", "sslSessionId", "userIp").block();
        Assertions.assertNull(authToken);
        client.reset();
    }

    @Test
    public void testOAuthOk(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedBlackboxResponse = new BlackboxOAuthResponse("OK", null,
                new BlackboxOAuthResponse.Status(BlackboxOAuthResponse.Status.VALID, "VALID"),
                new BlackboxOAuthResponse.Uid("1"), new BlackboxOAuthResponse.OAuth("2", "name",
                "quota-management:use tracker:read"));
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        YaAuthenticationToken authToken = authChecker.checkOauthToken("oauthToken", "userIp").block();
        Assertions.assertNotNull(authToken);
        YaPrincipal principal = (YaPrincipal) authToken.getPrincipal();
        Assertions.assertNotNull(principal);
        Assertions.assertFalse(principal.getUid().isEmpty());
        Assertions.assertFalse(principal.getOAuthClientId().isEmpty());
        Assertions.assertFalse(principal.getOAuthClientName().isEmpty());
        Assertions.assertFalse(principal.getTvmServiceId().isPresent());
        Assertions.assertEquals("1", principal.getUid().get());
        Assertions.assertEquals("2", principal.getOAuthClientId().get());
        Assertions.assertEquals("name", principal.getOAuthClientName().get());
        client.reset();
    }

    @Test
    public void testSessionIdSecureNotOk(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedBlackboxResponse = new BlackboxSessionIdResponse("Invalid", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.INVALID, "INVALID"),
                null, null);
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        YaAuthenticationToken authToken = authChecker.checkSessionId("sessionId", "sslSessionId", "userIp").block();
        Assertions.assertNull(authToken);
        client.reset();
    }

    @Test
    public void testOAuthNotOk(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedBlackboxResponse = new BlackboxOAuthResponse("Invalid", null,
                new BlackboxOAuthResponse.Status(BlackboxOAuthResponse.Status.INVALID, "INVALID"),
                null, null);
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        YaAuthenticationToken authToken = authChecker.checkOauthToken("oauthToken", "userIp").block();
        Assertions.assertNull(authToken);
        client.reset();
    }

    @Test
    public void testSessionIdSecureError(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedBlackboxResponse = new BlackboxSessionIdResponse("Blackbox error",
                new BlackboxSessionIdResponse.BlackboxException(
                        BlackboxSessionIdResponse.BlackboxException.DB_EXCEPTION, "DB_EXCEPTION"), null, null, null);
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkSessionId("sessionId", "sslSessionId", "userIp").block());
        client.reset();
    }

    @Test
    public void testOAuthError(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedBlackboxResponse = new BlackboxOAuthResponse("Error",
                new BlackboxOAuthResponse.BlackboxException(BlackboxOAuthResponse.BlackboxException.DB_EXCEPTION,
                        "DB_EXCEPTION"), null, null, null);
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkOauthToken("oauthToken", "userIp").block());
        client.reset();
    }

    @Test
    public void testSessionIdSecureOkCached(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedBlackboxResponse = new BlackboxSessionIdResponse("OK", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.VALID, "VALID"),
                new BlackboxSessionIdResponse.Uid("1"), new BlackboxSessionIdResponse.Auth(true));
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        YaAuthenticationToken authTokenFirst = authChecker.checkSessionId("sessionId", "sslSessionId", "userIp")
                .block();
        Assertions.assertNotNull(authTokenFirst);
        YaPrincipal principalFirst = (YaPrincipal) authTokenFirst.getPrincipal();
        Assertions.assertNotNull(principalFirst);
        Assertions.assertFalse(principalFirst.getUid().isEmpty());
        Assertions.assertTrue(principalFirst.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principalFirst.getOAuthClientName().isEmpty());
        Assertions.assertFalse(principalFirst.getTvmServiceId().isPresent());
        Assertions.assertEquals("1", principalFirst.getUid().get());
        YaAuthenticationToken authTokenSecond = authChecker.checkSessionId("sessionId", "sslSessionId", "userIp")
                .block();
        Assertions.assertNotNull(authTokenSecond);
        YaPrincipal principalSecond = (YaPrincipal) authTokenSecond.getPrincipal();
        Assertions.assertNotNull(principalSecond);
        Assertions.assertFalse(principalSecond.getUid().isEmpty());
        Assertions.assertTrue(principalSecond.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principalSecond.getOAuthClientName().isEmpty());
        Assertions.assertFalse(principalSecond.getTvmServiceId().isPresent());
        Assertions.assertEquals("1", principalSecond.getUid().get());
        client.reset();
    }

    @Test
    public void testOAuthOkCached(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedBlackboxResponse = new BlackboxOAuthResponse("OK", null,
                new BlackboxOAuthResponse.Status(BlackboxOAuthResponse.Status.VALID, "VALID"),
                new BlackboxOAuthResponse.Uid("1"), new BlackboxOAuthResponse.OAuth("2", "name",
                "quota-management:use tracker:read"));
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        YaAuthenticationToken authTokenFirst = authChecker.checkOauthToken("oauthToken", "userIp").block();
        Assertions.assertNotNull(authTokenFirst);
        YaPrincipal principalFirst = (YaPrincipal) authTokenFirst.getPrincipal();
        Assertions.assertNotNull(principalFirst);
        Assertions.assertFalse(principalFirst.getUid().isEmpty());
        Assertions.assertFalse(principalFirst.getOAuthClientId().isEmpty());
        Assertions.assertFalse(principalFirst.getOAuthClientName().isEmpty());
        Assertions.assertFalse(principalFirst.getTvmServiceId().isPresent());
        Assertions.assertEquals("1", principalFirst.getUid().get());
        Assertions.assertEquals("2", principalFirst.getOAuthClientId().get());
        Assertions.assertEquals("name", principalFirst.getOAuthClientName().get());
        YaAuthenticationToken authTokenSecond = authChecker.checkOauthToken("oauthToken", "userIp").block();
        Assertions.assertNotNull(authTokenSecond);
        YaPrincipal principalSecond = (YaPrincipal) authTokenFirst.getPrincipal();
        Assertions.assertNotNull(principalSecond);
        Assertions.assertFalse(principalSecond.getUid().isEmpty());
        Assertions.assertFalse(principalSecond.getOAuthClientId().isEmpty());
        Assertions.assertFalse(principalSecond.getOAuthClientName().isEmpty());
        Assertions.assertFalse(principalSecond.getTvmServiceId().isPresent());
        Assertions.assertEquals("1", principalSecond.getUid().get());
        Assertions.assertEquals("2", principalSecond.getOAuthClientId().get());
        Assertions.assertEquals("name", principalSecond.getOAuthClientName().get());
        client.reset();
    }

    @Test
    public void testSessionIdSecureNotOkCached(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedBlackboxResponse = new BlackboxSessionIdResponse("Invalid", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.INVALID, "INVALID"),
                null, null);
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        YaAuthenticationToken authTokenFirst = authChecker.checkSessionId("sessionId", "sslSessionId", "userIp")
                .block();
        Assertions.assertNull(authTokenFirst);
        YaAuthenticationToken authTokenSecond = authChecker.checkSessionId("sessionId", "sslSessionId", "userIp")
                .block();
        Assertions.assertNull(authTokenSecond);
        client.reset();
    }

    @Test
    public void testOAuthNotOkCached(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedBlackboxResponse = new BlackboxOAuthResponse("Invalid", null,
                new BlackboxOAuthResponse.Status(BlackboxOAuthResponse.Status.INVALID, "INVALID"),
                null, null);
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        YaAuthenticationToken authTokenFirst = authChecker.checkOauthToken("oauthToken", "userIp").block();
        Assertions.assertNull(authTokenFirst);
        YaAuthenticationToken authTokenSecond = authChecker.checkOauthToken("oauthToken", "userIp").block();
        Assertions.assertNull(authTokenSecond);
        client.reset();
    }

    @Test
    public void testSessionIdSecureTvmError(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedBlackboxResponse = new BlackboxSessionIdResponse("OK", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.VALID, "VALID"),
                new BlackboxSessionIdResponse.Uid("1"), new BlackboxSessionIdResponse.Auth(true));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkSessionId("sessionId", "sslSessionId", "userIp").block());
        client.reset();
    }

    @Test
    public void testOAuthTvmError(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedBlackboxResponse = new BlackboxOAuthResponse("OK", null,
                new BlackboxOAuthResponse.Status(BlackboxOAuthResponse.Status.VALID, "VALID"),
                new BlackboxOAuthResponse.Uid("1"), new BlackboxOAuthResponse.OAuth("2", "name",
                "quota-management:use tracker:read"));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkOauthToken("oauthToken", "userIp").block());
        client.reset();
    }

    @Test
    public void testSessionIdSecureErrorsNotCached(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedGoodBlackboxResponse = new BlackboxSessionIdResponse("OK", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.VALID, "VALID"),
                new BlackboxSessionIdResponse.Uid("1"), new BlackboxSessionIdResponse.Auth(true));
        BlackboxSessionIdResponse expectedBadBlackboxResponse = new BlackboxSessionIdResponse("Blackbox error",
                new BlackboxSessionIdResponse.BlackboxException(
                        BlackboxSessionIdResponse.BlackboxException.DB_EXCEPTION, "DB_EXCEPTION"), null, null, null);
        Map<String, TvmTicket> expectedGoodTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkSessionId("sessionId", "sslSessionId", "userIp").block());
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedGoodTvmResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedBadBlackboxResponse))
        );
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkSessionId("sessionId", "sslSessionId", "userIp").block());
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedGoodTvmResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedGoodBlackboxResponse))
        );
        YaAuthenticationToken authToken = authChecker.checkSessionId("sessionId", "sslSessionId", "userIp").block();
        Assertions.assertNotNull(authToken);
        YaPrincipal principal = (YaPrincipal) authToken.getPrincipal();
        Assertions.assertNotNull(principal);
        Assertions.assertFalse(principal.getUid().isEmpty());
        Assertions.assertTrue(principal.getOAuthClientId().isEmpty());
        Assertions.assertTrue(principal.getOAuthClientName().isEmpty());
        Assertions.assertFalse(principal.getTvmServiceId().isPresent());
        Assertions.assertEquals("1", principal.getUid().get());
        client.reset();
    }

    @Test
    public void testOAuthOkErrorsNotCached(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedGoodBlackboxResponse = new BlackboxOAuthResponse("OK", null,
                new BlackboxOAuthResponse.Status(BlackboxOAuthResponse.Status.VALID, "VALID"),
                new BlackboxOAuthResponse.Uid("1"), new BlackboxOAuthResponse.OAuth("2", "name",
                "quota-management:use tracker:read"));
        BlackboxOAuthResponse expectedBadBlackboxResponse = new BlackboxOAuthResponse("Error",
                new BlackboxOAuthResponse.BlackboxException(BlackboxOAuthResponse.BlackboxException.DB_EXCEPTION,
                        "DB_EXCEPTION"), null, null, null);
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkOauthToken("oauthToken", "userIp").block());
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedBadBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkOauthToken("oauthToken", "userIp").block());
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedGoodBlackboxResponse))
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        YaAuthenticationToken authToken = authChecker.checkOauthToken("oauthToken", "userIp").block();
        Assertions.assertNotNull(authToken);
        YaPrincipal principal = (YaPrincipal) authToken.getPrincipal();
        Assertions.assertNotNull(principal);
        Assertions.assertFalse(principal.getUid().isEmpty());
        Assertions.assertFalse(principal.getOAuthClientId().isEmpty());
        Assertions.assertFalse(principal.getOAuthClientName().isEmpty());
        Assertions.assertFalse(principal.getTvmServiceId().isPresent());
        Assertions.assertEquals("1", principal.getUid().get());
        Assertions.assertEquals("2", principal.getOAuthClientId().get());
        Assertions.assertEquals("name", principal.getOAuthClientName().get());
        client.reset();
    }

    @Test
    public void testSessionIdSecureFailure(MockServerClient client) throws JsonProcessingException {
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "yandex-team.ru")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("Error")
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkSessionId("sessionId", "sslSessionId", "userIp").block());
        client.reset();
    }

    @Test
    public void testOAuthFailure(MockServerClient client) throws JsonProcessingException {
        Map<String, TvmTicket> expectedTvmResponse = Map.of(
                "blackbox", new TvmTicket("serviceTicket", 223L, null));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(2)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("Error")
        );
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/tvm/tickets")
                        .withQueryStringParameter("src", "0")
                        .withQueryStringParameter("dsts", "223")
                        .withHeader("Authorization", "authtoken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(new TypeReference<Map<String, TvmTicket>>() { })
                        .writeValueAsString(expectedTvmResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        TvmClient tvmClient = new TvmClient(5000, 5000, 5000,
                "d", new TvmClientParams("http://" + host + ":" + port, "authtoken"));
        BlackboxAuthChecker authChecker = new BlackboxAuthChecker(tvmClient, blackboxClient, 0L,
                "quota-management:use", 223L, "yandex-team.ru");
        Assertions.assertThrows(IllegalStateException.class,
                () -> authChecker.checkOauthToken("oauthToken", "userIp").block());
        client.reset();
    }

}
