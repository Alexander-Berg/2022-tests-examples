package ru.yandex.vertis.tvm

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.io.Source
import spray.json.{enrichString, DeserializationException}

/**
  * @author alex-kovalenko
  */
class TvmJsonProtocolSpec extends TvmSpecBase with TvmJsonProtocol {

  "TvmJsonProtocol" should {
    "parse correct response" in {
      val expectedTickets = Seq(
        19 -> "service_ticket_1",
        213 -> "service_ticket_2",
        185 -> "service_ticket_3"
      )
      val json = load("/tvm/response.json").parseJson
      val response = json.convertTo[TvmClient.TicketResponse]
      response.tickets.toSeq should (have size 3 and contain theSameElementsAs expectedTickets)
    }
    "fail if has error in response" in {
      val json = load("/tvm/response_with_error.json").parseJson
      intercept[DeserializationException] {
        json.convertTo[TvmClient.TicketResponse]
      }
    }
  }
}
