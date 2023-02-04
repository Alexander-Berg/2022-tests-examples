package ru.yandex.vos2.api.handlers.util

import akka.http.scaladsl.model.ContentTypes.`text/csv(UTF-8)`
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.tracing.Traced
import ru.yandex.vos2.api.managers.util.{EmergencyRescheduleManager, RescheduleOffer}
import ru.yandex.vos2.api.utils.{DomainExceptionHandler, DomainRejectionHandler}

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class EmergencyRescheduleHandlerSpec
  extends WordSpec
  with HandlerSpecBase
  with DomainExceptionHandler
  with DomainRejectionHandler
  with PredefinedFromEntityUnmarshallers {

  val manager = mock[EmergencyRescheduleManager]

  override def cleanUp(): Unit = {
    system.terminate()
  }

  override protected val exceptionHandler: ExceptionHandler = specificExceptionHandler

  override protected val rejectionHandler: RejectionHandler = specificRejectionHandler

  override def routeUnderTest: Route =
    new EmergencyRescheduleHandler(manager).route

  "EmergencyRescheduleHandler" when {
    val offers = Seq(RescheduleOffer("100000273", "281475848178432"))
    "save items to db" should {
      (manager
        .reschedule(_: Seq[RescheduleOffer])(_: Traced))
        .expects(offers, *)
        .returning(Future.unit)
      "succeed" in {
        Put(
          "/utils/emergency-reschedule",
          HttpEntity(`text/csv(UTF-8)`, "100000273,281475848178432")
        ) ~>
          addHeader("Content-Type", "text/csv") ~> route ~> check {
          status shouldBe StatusCodes.Created
        }
      }
    }
  }
}
