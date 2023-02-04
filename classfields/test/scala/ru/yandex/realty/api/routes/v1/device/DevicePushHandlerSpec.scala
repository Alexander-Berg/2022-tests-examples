package ru.yandex.realty.api.routes.v1.device

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.model.headers.{`User-Agent`, RawHeader}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.akka.http.directives.PlatformExtractionDirectives.VertisPlatformHeader
import ru.yandex.realty.akka.http.directives.RequestDirectives
import ru.yandex.realty.api.routes.{defaultExceptionHandler, defaultRejectionHandler}
import ru.yandex.realty.api.ProtoResponse.RequiredFeatureResponse
import ru.yandex.realty.api.directives.MobileVersionComparator.{RealtyAndroidUserAgentPrefix, RealtyIosUserAgentPrefix}
import ru.yandex.realty.clients.laas.LaasClient
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.device.DevicePushManager
import ru.yandex.realty.pushnoy.PushnoyClient
import ru.yandex.realty.service.NonCheckingApplicationService

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class DevicePushHandlerSpec extends HandlerSpecBase {

  override def routeUnderTest: Route =
    RequestDirectives.pushRequestFrame(new NonCheckingApplicationService()) {
      new DevicePushHandler(
        new DevicePushManager(
          mock[PushnoyClient],
          mock[LaasClient]
        )
      ).route
    }
  override protected def exceptionHandler: ExceptionHandler = defaultExceptionHandler
  override protected def rejectionHandler: RejectionHandler = defaultRejectionHandler

  "/requiredFeature" should {

    def android(version: String): Seq[HttpHeader] = Seq(
      RawHeader(VertisPlatformHeader, "android/xxhdpi"),
      `User-Agent`(s"$RealtyAndroidUserAgentPrefix/$version (samsung SM-G955F; Android 9)")
    )
    def ios(version: String): Seq[HttpHeader] = Seq(
      RawHeader(VertisPlatformHeader, "ios/2x"),
      `User-Agent`(s"$RealtyIosUserAgentPrefix/$version (Apple x86_64; iOS 14.4)")
    )
    val PassportConfirmation = Seq("CONFIRM_PHONE_AND_EMAIL_VIA_PASSPORT")

    "not require CONFIRM_PHONE_AND_EMAIL_VIA_PASSPORT for Android 5.4.0" in {
      Get("/device/requiredFeature").withHeaders(android("5.4.0"): _*) ~> route ~> check {
        status shouldBe StatusCodes.OK
        entityAs[RequiredFeatureResponse].getResponse.getRentFlatDraftList.asScala shouldBe Seq.empty
      }
    }

    // When the non-passport version with the form is released, put that version in RentFlatDraftMinimalVersions
    // and add a test for not requiring the feature.

    // When the passport version with the form is released, put that version in
    // RentFlatDraftPassportConfirmationVersions, modify the previously added test to require the feature,
    // and add a new test for newer versions not requiring it.

    "not require CONFIRM_PHONE_AND_EMAIL_VIA_PASSPORT for iOS 305.5.0.1" in {
      Get("/device/requiredFeature").withHeaders(ios("305.5.0.1"): _*) ~> route ~> check {
        status shouldBe StatusCodes.OK
        entityAs[RequiredFeatureResponse].getResponse.getRentFlatDraftList.asScala shouldBe Seq.empty
      }
    }

    // See comments above and do the same for iOS.

  }

}
