package ru.yandex.vertis.tvm.impl

import akka.http.scaladsl.model.StatusCodes
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.actor.ActorSpecBase
import ru.yandex.vertis.billing.banker.util.{AkkaHttpTestUtils, DateTimeUtils}
import ru.yandex.vertis.tvm.TvmClient.GrantTypes
import ru.yandex.vertis.tvm.{TvmClient, TvmServerException, TvmSpecBase}

import scala.concurrent.duration.FiniteDuration
import scala.util.Failure

/**
  * @author alex-kovalenko
  */
class AkkaHttpTvmClientSpec
  extends ActorSpecBase("AkkaHttpTvmClientSpec")
  with TvmSpecBase
  with AkkaHttpTestUtils
  with AsyncSpecBase {

  implicit val timeout: FiniteDuration = FiniteDuration(3, "seconds")

  val responder = new MockHttpResponder()

  val client = new AkkaHttpTvmClient("fake", responder)

  override def beforeEach(): Unit = {
    responder.reset()
    super.beforeEach()
  }

  val dst = List(1, 2)

  val ts = DateTimeUtils.now()
  val unixTS = ts.getMillis / 1000

  val sign = "signature"

  val request = TvmClient.TicketRequest(SelfClientId, dst, ts, sign, GrantTypes.ClientCredentials)

  "AkkaHttpTvmClient" should {
    "request for ticket" in {
      val expectedParams = Seq(
        "grant_type" -> "client_credentials",
        "src" -> SelfClientId.toString,
        "dst" -> "1,2",
        "sign" -> sign,
        "ts" -> unixTS.toString
      )
      val expectedTickets = Map(
        19 -> "service_ticket_1",
        213 -> "service_ticket_2",
        185 -> "service_ticket_3"
      )
      responder.expectMultipartUrlencoded(expectedParams)
      responder.respondWithJson(TvmClient.`text/json`, load("/tvm/response.json"))

      val ticketsResponse = client.getTickets(request).futureValue
      ticketsResponse.tickets should contain theSameElementsAs expectedTickets
    }
    "throw IllegalArgumentException on 400" in {
      responder.respondWithStatus(StatusCodes.BadRequest)

      client.getTickets(request).toTry should matchPattern { case Failure(_: IllegalArgumentException) =>
      }
    }
    "throw TvmServerException on server errors" in {
      responder.respondWithStatus(StatusCodes.InternalServerError)

      client.getTickets(request).toTry should matchPattern { case Failure(_: TvmServerException) =>
      }
    }
    "throw RuntimeException on unknown error" in {
      responder.respondWithStatus(StatusCodes.ImATeapot)

      client.getTickets(request).toTry should matchPattern { case Failure(_: RuntimeException) =>
      }
    }
  }
}
