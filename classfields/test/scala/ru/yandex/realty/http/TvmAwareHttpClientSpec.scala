package ru.yandex.realty.http

import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.junit.runner.RunWith
import org.scalacheck.ShrinkLowPriority
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.tvm.TvmLibraryApi
import ru.yandex.realty.util.HttpClient.{ResponseFormat, UnitResponseFormat}
import ru.yandex.vertis.generators.BasicGenerators

@RunWith(classOf[JUnitRunner])
class TvmAwareHttpClientSpec extends AsyncSpecBase with PropertyChecks with BasicGenerators with ShrinkLowPriority {

  private val handler: PartialFunction[Int, ResponseFormat[Unit]] = {
    case 200 => UnitResponseFormat
  }

  private val pqBase = PreparedRequest[Unit](
    serviceName = "test-service",
    routeName = "test-route",
    endpoint = HttpEndpoint(""),
    request = new HttpGet(),
    format = ResponseHandler.newBuilder[Unit].handle(handler).build,
    requestConfig = RemoteHttpService.DefaultRequestConfig,
    errorReservoir = HttpRoute.newDefaultReservoir(),
    requestBodyForLogging = ""
  )

  private val tvmLibrary = mock[TvmLibraryApi]

  private val client = new MockHttpClient with TvmAwareHttpClient {
    override def tvm: TvmLibraryApi = tvmLibrary
  }

  before {
    client.reset()
  }

  after {
    client.reset()
  }

  "TvmAwareHttpClient" should {
    "skip tvm if has no context" in {
      client.respond(HttpStatus.SC_OK)
      client.doRequest(pqBase).futureValue.result.isSuccess should be(true)
    }

    "fail if there is no ticket for service" in {
      forAll(readableString) { service =>
        val ctx = TvmContext(service)
        val ex = new RuntimeException("artificial")
        (tvmLibrary.getTicket _).expects(service).throwing(ex)
        interceptCause[RuntimeException] {
          client.doRequest(pqBase.copy(tvmContext = Some(ctx))).futureValue
        }
      }
    }

    "set tvm header" in {
      forAll(readableString, readableString) { (service, ticket) =>
        client.reset()
        val ctx = TvmContext(service)
        (tvmLibrary.getTicket _).expects(service).returning(ticket)
        client.expectHeader(XYaServiceTicketHeaderName, ticket)
        client.respond(HttpStatus.SC_OK)
        client.doRequest(pqBase.copy(tvmContext = Some(ctx))).futureValue.result.isSuccess should be(true)
      }
    }
  }
}
