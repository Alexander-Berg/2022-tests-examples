package ru.yandex.intranet.d.web.security.blackbox;

import java.util.List;
import java.util.Set;

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
import org.springframework.web.reactive.function.client.WebClientException;

import ru.yandex.intranet.d.web.security.blackbox.model.BlackboxException;
import ru.yandex.intranet.d.web.security.blackbox.model.BlackboxOAuthResponse;
import ru.yandex.intranet.d.web.security.blackbox.model.BlackboxSessionIdResponse;
import ru.yandex.intranet.d.web.security.blackbox.model.CheckedOAuthToken;
import ru.yandex.intranet.d.web.security.blackbox.model.CheckedSessionId;
import ru.yandex.intranet.d.web.security.blackbox.model.InvalidOAuthToken;
import ru.yandex.intranet.d.web.security.blackbox.model.InvalidSessionId;
import ru.yandex.intranet.d.web.security.blackbox.model.ValidOAuthToken;
import ru.yandex.intranet.d.web.security.blackbox.model.ValidSessionId;

/**
 * Blackbox client test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@ExtendWith(MockServerExtension.class)
public class BlackboxClientTest {

    @Test
    public void testSessionIdSecureOk(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedResponse = new BlackboxSessionIdResponse("OK", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.VALID, "VALID"),
                new BlackboxSessionIdResponse.Uid("1"), new BlackboxSessionIdResponse.Auth(true));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "host")
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
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        CheckedSessionId expectedResult = CheckedSessionId.valid(new ValidSessionId("1", false, true));
        Assertions.assertEquals(expectedResult, blackboxClient.sessionId("serviceTicket", "sessionId",
                "userIp", "host", "sslSessionId").block());
        client.reset();
    }

    @Test
    public void testSessionIdSecureOkNeedReset(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedResponse = new BlackboxSessionIdResponse("OK", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.NEED_RESET, "VALID"),
                new BlackboxSessionIdResponse.Uid("1"), new BlackboxSessionIdResponse.Auth(true));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "host")
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
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        CheckedSessionId expectedResult = CheckedSessionId.valid(new ValidSessionId("1", true, true));
        Assertions.assertEquals(expectedResult, blackboxClient.sessionId("serviceTicket", "sessionId",
                "userIp", "host", "sslSessionId").block());
        client.reset();
    }

    @Test
    public void testSessionIdNotSecureOk(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedResponse = new BlackboxSessionIdResponse("OK", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.VALID, "VALID"),
                new BlackboxSessionIdResponse.Uid("1"), new BlackboxSessionIdResponse.Auth(false));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "host")
                        .withQueryStringParameter("format", "json")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxSessionIdResponse.class)
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        CheckedSessionId expectedResult = CheckedSessionId.valid(new ValidSessionId("1", false, false));
        Assertions.assertEquals(expectedResult, blackboxClient.sessionId("serviceTicket", "sessionId",
                "userIp", "host", "sslSessionId").block());
        client.reset();
    }

    @Test
    public void testSessionIdNotOk(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedResponse = new BlackboxSessionIdResponse("Not valid", null,
                new BlackboxSessionIdResponse.Status(BlackboxSessionIdResponse.Status.INVALID, "INVALID"),
                null, null);
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "host")
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
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        CheckedSessionId expectedResult = CheckedSessionId.invalid(new InvalidSessionId("Not valid",
                new InvalidSessionId.Status(InvalidSessionId.Status.INVALID, "INVALID")));
        Assertions.assertEquals(expectedResult, blackboxClient.sessionId("serviceTicket", "sessionId",
                "userIp", "host", "sslSessionId").block());
        client.reset();
    }

    @Test
    public void testSessionIdBlackboxException(MockServerClient client) throws JsonProcessingException {
        BlackboxSessionIdResponse expectedResponse = new BlackboxSessionIdResponse("Blackbox error",
                new BlackboxSessionIdResponse.BlackboxException(
                        BlackboxSessionIdResponse.BlackboxException.DB_EXCEPTION, "DB_EXCEPTION"), null, null, null);
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "host")
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
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        Assertions.assertThrows(BlackboxException.class, () -> blackboxClient.sessionId("serviceTicket",
                "sessionId", "userIp", "host", "sslSessionId").block());
        client.reset();
    }

    @Test
    public void testSessionIdUnexpectedResponse(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "sessionid")
                        .withQueryStringParameter("sessionid", "sessionId")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("host", "host")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("sslsessionid", "sslSessionId")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        Assertions.assertThrows(WebClientException.class, () -> blackboxClient.sessionId("serviceTicket",
                "sessionId", "userIp", "host", "sslSessionId").block());
        client.reset();
    }

    @Test
    public void testOAuthOk(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedResponse = new BlackboxOAuthResponse("OK", null,
                new BlackboxOAuthResponse.Status(BlackboxOAuthResponse.Status.VALID, "VALID"),
                new BlackboxOAuthResponse.Uid("1"), new BlackboxOAuthResponse.OAuth("2", "name",
                "quota-management:use tracker:read"));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use,tracker:read")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        CheckedOAuthToken expectedResult = CheckedOAuthToken.valid(new ValidOAuthToken("1", "2",
                "name", Set.of("quota-management:use", "tracker:read")));
        Assertions.assertEquals(expectedResult, blackboxClient.oauth("serviceTicket", "oauthToken",
                "userIp", List.of("quota-management:use", "tracker:read")).block());
        client.reset();
    }

    @Test
    public void testOAuthWithoutScopesOk(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedResponse = new BlackboxOAuthResponse("OK", null,
                new BlackboxOAuthResponse.Status(BlackboxOAuthResponse.Status.VALID, "VALID"),
                new BlackboxOAuthResponse.Uid("1"), new BlackboxOAuthResponse.OAuth("2", "name",
                "quota-management:use tracker:read"));
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        CheckedOAuthToken expectedResult = CheckedOAuthToken.valid(new ValidOAuthToken("1", "2",
                "name", Set.of("quota-management:use", "tracker:read")));
        Assertions.assertEquals(expectedResult, blackboxClient.oauth("serviceTicket", "oauthToken",
                "userIp").block());
        client.reset();
    }

    @Test
    public void testOAuthNotOk(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedResponse = new BlackboxOAuthResponse("Not valid", null,
                new BlackboxOAuthResponse.Status(BlackboxOAuthResponse.Status.INVALID, "INVALID"),
                null, null);
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use,tracker:read")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        CheckedOAuthToken expectedResult = CheckedOAuthToken.invalid(new InvalidOAuthToken("Not valid",
                new InvalidOAuthToken.Status(InvalidOAuthToken.Status.INVALID, "INVALID")));
        Assertions.assertEquals(expectedResult, blackboxClient.oauth("serviceTicket", "oauthToken",
                "userIp", List.of("quota-management:use", "tracker:read")).block());
        client.reset();
    }

    @Test
    public void testOAuthBlackboxException(MockServerClient client) throws JsonProcessingException {
        BlackboxOAuthResponse expectedResponse = new BlackboxOAuthResponse("Error",
                new BlackboxOAuthResponse.BlackboxException(BlackboxOAuthResponse.BlackboxException.DB_EXCEPTION,
                        "DB_EXCEPTION"), null, null, null);
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use,tracker:read")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(new ObjectMapper().registerModule(new Jdk8Module())
                        .writerFor(BlackboxOAuthResponse.class)
                        .writeValueAsString(expectedResponse))
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        Assertions.assertThrows(BlackboxException.class, () -> blackboxClient.oauth("serviceTicket",
                "oauthToken", "userIp", List.of("quota-management:use", "tracker:read")).block());
        client.reset();
    }

    @Test
    public void testOAuthUnexpectedResponse(MockServerClient client) {
        client.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/blackbox")
                        .withQueryStringParameter("method", "oauth")
                        .withQueryStringParameter("userip", "userIp")
                        .withQueryStringParameter("format", "json")
                        .withQueryStringParameter("scopes", "quota-management:use,tracker:read")
                        .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                        .withHeader("Authorization", "OAuth oauthToken")
                        .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
                Times.exactly(1)
        ).respond(HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.PLAIN_TEXT_UTF_8)
                .withBody("Error")
        );
        String host = client.remoteAddress().getHostString();
        int port = client.remoteAddress().getPort();
        BlackboxClient blackboxClient = new BlackboxClient(5000, 5000, 5000,
                "http://" + host + ":" + port + "/blackbox", "d");
        Assertions.assertThrows(WebClientException.class, () -> blackboxClient.oauth("serviceTicket",
                "oauthToken", "userIp", List.of("quota-management:use", "tracker:read")).block());
        client.reset();
    }

}
