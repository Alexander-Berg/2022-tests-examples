package ru.yandex.intranet.d.services.integration.jns

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockserver.client.MockServerClient
import org.mockserver.junit.jupiter.MockServerExtension
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.JsonBody
import org.mockserver.model.JsonSchemaBody
import org.mockserver.model.MediaType
import ru.yandex.intranet.d.web.security.tvm.TvmClient
import ru.yandex.intranet.d.web.security.tvm.TvmClientParams
import ru.yandex.intranet.d.web.security.tvm.model.TvmTicket

/**
 * JNS client test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@ExtendWith(MockServerExtension::class)
class JnsClientTest {

    @Test
    fun testSendOk(client: MockServerClient) {
        val expectedTvmResponse = mapOf("blackbox" to TvmTicket("serviceTicket", 223L, null))
        client.`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/tvm/tickets")
                .withQueryStringParameter("src", "0")
                .withQueryStringParameter("dsts", "223")
                .withHeader("Authorization", "authtoken")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    ObjectMapper().registerModule(Jdk8Module())
                        .writerFor(object : TypeReference<Map<String?, TvmTicket?>?>() {})
                        .writeValueAsString(expectedTvmResponse)
                )
        )
        client.`when`(
            HttpRequest.request()
                .withMethod("POST")
                .withPath("/api/messages/send_to_channel_json")
                .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
                .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
                .withHeader("User-Agent", "d")
                .withBody(JsonSchemaBody("{type: 'object', properties: { 'project': { 'type': 'string' }, " +
                    "'template': { 'type': 'string' }, 'target_project': { 'type': 'string' }, " +
                    "'channel': { 'type': 'string' }, 'request_id': { 'type': 'string' }, " +
                    "'params': { 'type': 'object' } }, 'required': ['project', 'template', 'target_project', " +
                    "'channel', 'request_id', 'params']}")),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(JsonBody("{}"))
        )
        val host = client.remoteAddress().hostString
        val port = client.remoteAddress().port
        val tvmClient = TvmClient(
            5000, 5000, 5000,
            "d", TvmClientParams("http://$host:$port", "authtoken")
        )
        val objectMapper = JsonMapper.builder()
            .addModule(Jdk8Module())
            .addModule(JavaTimeModule())
            .addModule(ParameterNamesModule())
            .addModule(KotlinModule.Builder().build())
            .build()
        val jnsClient = JnsClientImpl(tvmClient, objectMapper, 223L, 0L, "http://$host:$port",
            5000, 5000L, 5000L, 5000L, 2L,
        1000L, "d")
        val result = runBlocking {
            jnsClient.send(JnsMessage("intranet_d", "default", "intranet_d",
                "test", mapOf("test" to "value")))
        }
        when (result) {
            is JnsResult.Success -> {}
            is JnsResult.Failure -> throw result.error
            is JnsResult.Error -> throw IllegalStateException("Error: $result")
        }
        client.reset()
    }

    @Test
    fun testSendConflict(client: MockServerClient) {
        val expectedTvmResponse = mapOf("blackbox" to TvmTicket("serviceTicket", 223L, null))
        client.`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/tvm/tickets")
                .withQueryStringParameter("src", "0")
                .withQueryStringParameter("dsts", "223")
                .withHeader("Authorization", "authtoken")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    ObjectMapper().registerModule(Jdk8Module())
                        .writerFor(object : TypeReference<Map<String?, TvmTicket?>?>() {})
                        .writeValueAsString(expectedTvmResponse)
                )
        )
        client.`when`(
            HttpRequest.request()
                .withMethod("POST")
                .withPath("/api/messages/send_to_channel_json")
                .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
                .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
                .withHeader("User-Agent", "d")
                .withBody(JsonSchemaBody("{type: 'object', properties: { 'project': { 'type': 'string' }, " +
                    "'template': { 'type': 'string' }, 'target_project': { 'type': 'string' }, " +
                    "'channel': { 'type': 'string' }, 'request_id': { 'type': 'string' }, " +
                    "'params': { 'type': 'object' } }, 'required': ['project', 'template', 'target_project', " +
                    "'channel', 'request_id', 'params']}")),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(409)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(JsonBody("{'code': 6, 'message': 'Conflict'}"))
        )
        val host = client.remoteAddress().hostString
        val port = client.remoteAddress().port
        val tvmClient = TvmClient(
            5000, 5000, 5000,
            "d", TvmClientParams("http://$host:$port", "authtoken")
        )
        val objectMapper = JsonMapper.builder()
            .addModule(Jdk8Module())
            .addModule(JavaTimeModule())
            .addModule(ParameterNamesModule())
            .addModule(KotlinModule.Builder().build())
            .build()
        val jnsClient = JnsClientImpl(tvmClient, objectMapper, 223L, 0L, "http://$host:$port",
            5000, 5000L, 5000L, 5000L, 2L,
            1000L, "d")
        val result = runBlocking {
            jnsClient.send(JnsMessage("intranet_d", "default", "intranet_d",
                "test", mapOf("test" to "value")))
        }
        when (result) {
            is JnsResult.Success -> {}
            is JnsResult.Failure -> throw result.error
            is JnsResult.Error -> throw IllegalStateException("Error: $result")
        }
        client.reset()
    }

    @Test
    fun testSendError(client: MockServerClient) {
        val expectedTvmResponse = mapOf("blackbox" to TvmTicket("serviceTicket", 223L, null))
        client.`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/tvm/tickets")
                .withQueryStringParameter("src", "0")
                .withQueryStringParameter("dsts", "223")
                .withHeader("Authorization", "authtoken")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    ObjectMapper().registerModule(Jdk8Module())
                        .writerFor(object : TypeReference<Map<String?, TvmTicket?>?>() {})
                        .writeValueAsString(expectedTvmResponse)
                )
        )
        client.`when`(
            HttpRequest.request()
                .withMethod("POST")
                .withPath("/api/messages/send_to_channel_json")
                .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
                .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
                .withHeader("User-Agent", "d")
                .withBody(JsonSchemaBody("{type: 'object', properties: { 'project': { 'type': 'string' }, " +
                    "'template': { 'type': 'string' }, 'target_project': { 'type': 'string' }, " +
                    "'channel': { 'type': 'string' }, 'request_id': { 'type': 'string' }, " +
                    "'params': { 'type': 'object' } }, 'required': ['project', 'template', 'target_project', " +
                    "'channel', 'request_id', 'params']}")),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(403)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(JsonBody("{'code': 7, 'message': 'no access'}"))
        )
        val host = client.remoteAddress().hostString
        val port = client.remoteAddress().port
        val tvmClient = TvmClient(
            5000, 5000, 5000,
            "d", TvmClientParams("http://$host:$port", "authtoken")
        )
        val objectMapper = JsonMapper.builder()
            .addModule(Jdk8Module())
            .addModule(JavaTimeModule())
            .addModule(ParameterNamesModule())
            .addModule(KotlinModule.Builder().build())
            .build()
        val jnsClient = JnsClientImpl(tvmClient, objectMapper, 223L, 0L, "http://$host:$port",
            5000, 5000L, 5000L, 5000L, 2L,
            1000L, "d")
        val result = runBlocking {
            jnsClient.send(JnsMessage("intranet_d", "default", "intranet_d",
                "test", mapOf("test" to "value")))
        }
        when (result) {
            is JnsResult.Success -> throw IllegalStateException("Unexpected success")
            is JnsResult.Failure -> throw result.error
            is JnsResult.Error -> {
                Assertions.assertEquals(JnsResponse(code = 7, message = "no access"), result.errorObject)
                Assertions.assertEquals(403, result.statusCode)
            }
        }
        client.reset()
    }

    @Test
    fun testSendErrorPlainText(client: MockServerClient) {
        val expectedTvmResponse = mapOf("blackbox" to TvmTicket("serviceTicket", 223L, null))
        client.`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/tvm/tickets")
                .withQueryStringParameter("src", "0")
                .withQueryStringParameter("dsts", "223")
                .withHeader("Authorization", "authtoken")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(
                    ObjectMapper().registerModule(Jdk8Module())
                        .writerFor(object : TypeReference<Map<String?, TvmTicket?>?>() {})
                        .writeValueAsString(expectedTvmResponse)
                )
        )
        client.`when`(
            HttpRequest.request()
                .withMethod("POST")
                .withPath("/api/messages/send_to_channel_json")
                .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
                .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
                .withHeader("User-Agent", "d")
                .withBody(JsonSchemaBody("{type: 'object', properties: { 'project': { 'type': 'string' }, " +
                    "'template': { 'type': 'string' }, 'target_project': { 'type': 'string' }, " +
                    "'channel': { 'type': 'string' }, 'request_id': { 'type': 'string' }, " +
                    "'params': { 'type': 'object' } }, 'required': ['project', 'template', 'target_project', " +
                    "'channel', 'request_id', 'params']}")),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(403)
                .withContentType(MediaType.TEXT_PLAIN)
                .withBody("Error")
        )
        val host = client.remoteAddress().hostString
        val port = client.remoteAddress().port
        val tvmClient = TvmClient(
            5000, 5000, 5000,
            "d", TvmClientParams("http://$host:$port", "authtoken")
        )
        val objectMapper = JsonMapper.builder()
            .addModule(Jdk8Module())
            .addModule(JavaTimeModule())
            .addModule(ParameterNamesModule())
            .addModule(KotlinModule.Builder().build())
            .build()
        val jnsClient = JnsClientImpl(tvmClient, objectMapper, 223L, 0L, "http://$host:$port",
            5000, 5000L, 5000L, 5000L, 2L,
            1000L, "d")
        val result = runBlocking {
            jnsClient.send(JnsMessage("intranet_d", "default", "intranet_d",
                "test", mapOf("test" to "value")))
        }
        when (result) {
            is JnsResult.Success -> throw IllegalStateException("Unexpected success")
            is JnsResult.Failure -> throw result.error
            is JnsResult.Error -> {
                Assertions.assertEquals("Error", result.errorText)
                Assertions.assertEquals(403, result.statusCode)
            }
        }
        client.reset()
    }

    @Test
    fun testSendTvmFailure(client: MockServerClient) {
        client.`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/tvm/tickets")
                .withQueryStringParameter("src", "0")
                .withQueryStringParameter("dsts", "223")
                .withHeader("Authorization", "authtoken")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString()),
            Times.exactly(2)
        ).respond(
            HttpResponse.response()
                .withStatusCode(500)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("Error")
        )
        client.`when`(
            HttpRequest.request()
                .withMethod("POST")
                .withPath("/api/messages/send_to_channel_json")
                .withHeader("X-Ya-Service-Ticket", "serviceTicket")
                .withHeader("Accept", MediaType.APPLICATION_JSON.toString())
                .withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
                .withHeader("User-Agent", "d")
                .withBody(JsonSchemaBody("{type: 'object', properties: { 'project': { 'type': 'string' }, " +
                    "'template': { 'type': 'string' }, 'target_project': { 'type': 'string' }, " +
                    "'channel': { 'type': 'string' }, 'request_id': { 'type': 'string' }, " +
                    "'params': { 'type': 'object' } }, 'required': ['project', 'template', 'target_project', " +
                    "'channel', 'request_id', 'params']}")),
            Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(JsonBody("{}"))
        )
        val host = client.remoteAddress().hostString
        val port = client.remoteAddress().port
        val tvmClient = TvmClient(
            5000, 5000, 5000,
            "d", TvmClientParams("http://$host:$port", "authtoken")
        )
        val objectMapper = JsonMapper.builder()
            .addModule(Jdk8Module())
            .addModule(JavaTimeModule())
            .addModule(ParameterNamesModule())
            .addModule(KotlinModule.Builder().build())
            .build()
        val jnsClient = JnsClientImpl(tvmClient, objectMapper, 223L, 0L, "http://$host:$port",
            5000, 5000L, 5000L, 5000L, 2L,
            1000L, "d")
        var exception = false
        try {
            runBlocking {
                jnsClient.send(JnsMessage("intranet_d", "default", "intranet_d",
                    "test", mapOf("test" to "value")))
            }
        } catch (e: Exception) {
            exception = true
        }
        Assertions.assertTrue(exception)
        client.reset()
    }

}
