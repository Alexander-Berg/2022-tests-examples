package ru.yandex.realty.api.routes.v1.export

import akka.http.scaladsl.model.{ContentType, HttpCharsets, MediaType, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.api.routes._
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.export.ExportManager
import ru.yandex.realty.model.user.UserRefGenerators
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * @author abulychev
  */
@RunWith(classOf[JUnitRunner])
class ExportHandlerSpec extends HandlerSpecBase with PropertyChecks with UserRefGenerators {

  private val manager: ExportManager = mock[ExportManager]

  override def routeUnderTest: Route = new ExportHandler(manager).route

  private val jsonType = ContentType(MediaType.applicationWithFixedCharset("json", HttpCharsets.`UTF-8`, "json"))

  "POST /export/offers.xls" should {
    val request = Post("/export/offers.xls")

    "succeed on defined users" in {
      (manager
        .exportToExcel(_: ExportParams)(_: Traced))
        .expects(
          ExportParams(
            offerIds = List("4056522032826314496", "7688171018480025601", "6302897158638950130", "1187691561166351360"),
            link = true,
            contacts = true,
            archive = false
          ),
          *
        )
        .returning(Future.successful(Array.emptyByteArray))

      request
        .withEntity(
          jsonType,
          """{
            |"offerIds": ["4056522032826314496", "7688171018480025601", "6302897158638950130", "1187691561166351360"],
            |"link": true,
            |"contacts": true,
            |"archive": false
            |}""".stripMargin.getBytes
        ) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }
  }

  override protected val exceptionHandler: ExceptionHandler = defaultExceptionHandler

  override protected val rejectionHandler: RejectionHandler = defaultRejectionHandler
}
