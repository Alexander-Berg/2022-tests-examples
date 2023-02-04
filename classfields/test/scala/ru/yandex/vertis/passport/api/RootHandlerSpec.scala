package ru.yandex.vertis.passport.api

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.`WWW-Authenticate`
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.WordSpec
import ru.yandex.passport.tvmauth.CheckedServiceTicket
import ru.yandex.passport.tvmauth.TicketStatus
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.passport.service.tvm.UserTvmService
import ru.yandex.vertis.passport.test.MockFeatures.featureOff
import ru.yandex.vertis.passport.test.MockFeatures.featureOn
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.tvm.`X-Ya-Service-Ticket`

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class RootHandlerSpec extends WordSpec with RootedSpecBase with MockedBackend with OptionValues {

  "/ping" should {
    "return OK" in {
      when(featureManager.TvmAuthorization).thenReturn(featureOn)
      Get("/ping") ~> route ~> check {
        status shouldBe OK
      }
    }
  }

  "/swagger/" should {
    "return swagger html" in {
      when(featureManager.TvmAuthorization).thenReturn(featureOn)
      Get("/swagger/") ~> route ~> check {
        status shouldBe OK
        contentType shouldBe ContentTypes.`text/html(UTF-8)`
      }
    }
  }

  "on completely wrong path" should {
    "return NotFound" in {
      when(featureManager.TvmAuthorization).thenReturn(featureOn)
      Get("/something-wrong") ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "/api/" should {
    "authorize the client if TVM is enabled" in {
      val ticket = Gen.identifier.next
      when(featureManager.TvmAuthorization).thenReturn(featureOn)
      when(tvmTicketChecker.checkServiceTicket(eq(ticket)))
        .thenReturn(Future.successful(new CheckedServiceTicket(TicketStatus.OK, "", 0, 0)))

      Get("/api") ~>
        addHeader("X-Ya-Service-Ticket", ticket) ~>
        route ~> check {
        status shouldBe NotFound
      }
    }

    "fail authorization if the ticket fails validation" in {
      val ticket = Gen.identifier.next
      when(featureManager.TvmAuthorization).thenReturn(featureOn)
      when(tvmTicketChecker.checkServiceTicket(eq(ticket)))
        .thenReturn(Future.successful(new CheckedServiceTicket(TicketStatus.EXPIRED, "", 0, 0)))

      Get("/api") ~>
        addHeader("X-Ya-Service-Ticket", ticket) ~>
        route ~> check {
        status shouldBe Unauthorized
      }
    }

    "fail authorization if the header is missing" in {
      when(featureManager.TvmAuthorization).thenReturn(featureOn)

      Get("/api") ~> route ~> check {
        status shouldBe Unauthorized
      }
    }

    "skip authorization if TVM is disabled (no header)" in {
      when(featureManager.TvmAuthorization).thenReturn(featureOff)

      Get("/api") ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "skip authorization if TVM is disabled (bad ticket)" in {
      val ticket = Gen.identifier.next
      when(featureManager.TvmAuthorization).thenReturn(featureOff)
      when(tvmTicketChecker.checkServiceTicket(eq(ticket)))
        .thenReturn(Future.successful(new CheckedServiceTicket(TicketStatus.EXPIRED, "", 0, 0)))

      Get("/api") ~>
        addHeader("X-Ya-Service-Ticket", ticket) ~>
        route ~>
        check {
          status shouldBe NotFound
        }
    }
  }
}
