package ru.yandex.vertis.scoring.taxi.client

import cats.implicits.catsSyntaxApplicativeId
import eu.timepit.refined.{refineMV, refineV}
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.quality.tvm_utils.TvmTicketProvider
import ru.yandex.vertis.scoring.model.{Phone, PhoneId}
import sttp.client.{Identity, NothingT, RequestT, Response, StringBody, SttpBackend}
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import eu.timepit.refined.auto._
import io.circe.parser.parse
import io.circe._
import ru.yandex.vertis.quality.tvm_utils.TvmTicketProvider.TvmTicketHeader
import ru.yandex.vertis.scoring.client.taxi.impl.HttpTaxiClient
import ru.yandex.vertis.scoring.converters.TaxiFormat._
import ru.yandex.vertis.scoring.model.taxi.request.PhonesBulkRequest

class HttpTaxiClientSpec extends SpecBase {

  import HttpTaxiClientSpec._

  implicit private val stubBackend = mock[SttpBackend[F, Nothing, NothingT]]
  stub(stubBackend.send(_: RequestT[Identity, String, Nothing])) { case request: RequestT[Identity, String, Nothing] =>
    getResponse(request).pure
  }

  private val tvmTicketProvider = mock[TvmTicketProvider[F]]
  when(tvmTicketProvider.getTvmTicket(?)).thenReturn("ticket".pure)

  private val stubClient = new HttpTaxiClient("http://test.test.net/test", tvmTicketProvider, 1)

  private val tests: List[TestCase] =
    List(
      TestCase(List("+71112223344"), List("123"), "handle response with 1 result"),
      TestCase(List("+71112223388"), Nil, "handle empty response"),
      TestCase(List("+71112223344", "+71112223355"), List("123", "124"), "handle response with many results")
    )

  "HttpTaxiClient" should {

    tests.foreach(
    test =>
      test.desc in {
        stubClient.bulkGetByPhone(test.phones).await shouldBe test.ids
      })
  }
}

object HttpTaxiClientSpec {

  private def getResponse(request: RequestT[Identity, String, String]): Response[String] = {
    val body = request.body.asInstanceOf[StringBody]

    assert(request.headers.exists(_.name == TvmTicketHeader))

    val items = parse(body.s).toOption.flatMap(_.as[PhonesBulkRequest].toOption).get.items

    if (items.size > 1) {
      Response.ok(multipleResponse)
    } else if (items.head.value.toString() == "+71112223344") {
      Response.ok(singleResponse)
    } else {
      Response.ok(emptyResponse)
    }
  }

  val emptyResponse =
    """{
      | "items": [
      |
      | ]
      |}""".stripMargin

  val singleResponse =
    """{
      | "items": [
      |   {
      |     "id": "123",
      |     "value" : "+71112223344"
      |   }
      | ]
      |}""".stripMargin

  val multipleResponse =
    """{
      | "items": [
      |   {
      |     "id": "123",
      |     "value" : "+71112223344"
      |   },
      |   {
      |     "id": "124",
      |     "value" : "+71112223355"
      |   }
      |
      | ]
      |}""".stripMargin

  case class TestCase(phones: Seq[Phone], ids: Seq[PhoneId], desc: String)

}
