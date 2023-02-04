package ru.yandex.realty.api.directives

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{ProductVersion, RawHeader}
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import com.typesafe.config.{Config, ConfigFactory}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.akka.http.handlers.BaseHandler
import ru.yandex.realty.api.routes._
import ru.yandex.realty.api.{ApiExceptionHandler, ApiRejectionHandler}
import ru.yandex.realty.application.ng.{DefaultConfigProvider, TypesafeConfigProvider}
import ru.yandex.realty.auth.{Application, TamperDirective}
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.ops.DefaultOperationalComponents
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.model.user.AppUser
import ru.yandex.realty.platform.PlatformInfo
import ru.yandex.realty.pushnoy.model.ClientOS
import ru.yandex.realty.request.UserAgent
import ru.yandex.realty.util.TestPropertiesSetup

import scala.concurrent.ExecutionContext
import scala.languageFeature.postfixOps

@RunWith(classOf[JUnitRunner])
class TamperingDirectiveSpec
  extends HandlerSpecBase
  with TypesafeConfigProvider
  with TestPropertiesSetup // must be mixed before TamperDirective (because of Properties initialization)
  with TamperDirective
  with DefaultOperationalComponents
  with FeaturesStubComponent {

  override protected val exceptionHandler: ExceptionHandler = ApiExceptionHandler.handler
  override protected val rejectionHandler: RejectionHandler = ApiRejectionHandler.handler
  override def typesafeConfig: Config = ConfigFactory.parseString("realty.tamper.salt=123")

  override def routeUnderTest: Route =
    withTamperCheck {
      new TestHandler().route
    }

  val uuid = "1234567"

  "Any HTTP GET" when {

    "'withTamperCheck' directive used & features.CheckTamper enabled" should {

      "return error for wrong tamper" in {
        features.CheckTamper.setNewState(true)

        makeTestRequest(Uri("/"), Some("badTamper")) ~>
          route ~>
          check {
            status should be(StatusCodes.TooManyRequests)
          }
      }

      "return error if no tamper" in {
        features.CheckTamper.setNewState(true)

        makeTestRequest(Uri("/"), None) ~>
          route ~>
          check {
            status should be(StatusCodes.TooManyRequests)
          }
      }

      "return ok for correct tamper" in {
        features.CheckTamper.setNewState(true)
        val params = Map("param1" -> "val1", "param2" -> "val2")

        makeTestRequest(Uri("/").withQuery(Query(params)), Some(calcTamperValue(uuid, params.toSeq))) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }

    "'withTamperCheck' directive used & features.CheckTamper disabled" should {

      "return ok for wrong tamper" in {
        features.CheckTamper.setNewState(false)

        makeTestRequest(Uri("/"), Some("BadTamper")) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }

      "return ok if no tamper" in {
        features.CheckTamper.setNewState(false)

        makeTestRequest(Uri("/"), None) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }

    "'application' is NOT 'regular'" should {

      "return ok for wrong tamper" in {
        features.CheckTamper.setNewState(true)

        Get(Uri("/"))
          .withHeaders(
            RawHeader(TamperHeaderName, "bad")
          )
          .withApplication(Application.RealtyFront)
          .withUser(AppUser(uuid))
          .acceptingProto ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }

      "return ok if no tamper" in {
        features.CheckTamper.setNewState(true)

        Get(Uri("/"))
          .withApplication(Application.RealtyFront)
          .withUser(AppUser(uuid))
          .acceptingProto ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }

    "old client version" should {

      // some time ago we did not require tamper for old clients, but we have changed it
      "return error if no tamper" in {
        features.CheckTamper.setNewState(true)

        makeTestRequestOldClient(Uri("/")) ~>
          route ~>
          check {
            status should be(StatusCodes.TooManyRequests)
          }
      }
    }
  }

  private def makeTestRequest(uri: Uri, tamperOpt: Option[String]) =
    tamperOpt
      .fold(Get(uri)) { tamper =>
        Get(uri).withHeaders(RawHeader(TamperHeaderName, tamper))
      }
      .withApplication(Application.Regular)
      .withUser(AppUser(uuid))
      .withPlatform(Some(PlatformInfo("ios", "3xx")))
      .withUserAgent(
        Some(
          UserAgent(
            Seq(
              ProductVersion(
                MobileVersionComparator.RealtyIosUserAgentPrefix,
                MobileVersionComparator.tamperClientsMinimumVersions(ClientOS.IOS),
                ""
              )
            )
          )
        )
      )
      .acceptingProto

  private def makeTestRequestOldClient(uri: Uri) =
    Get(uri)
      .withApplication(Application.Regular)
      .withUser(AppUser(uuid))
      .withPlatform(Some(PlatformInfo("ios", "3xx")))
      .withUserAgent(
        Some(UserAgent(Seq(ProductVersion(MobileVersionComparator.RealtyIosUserAgentPrefix, "1.0.0", ""))))
      )
      .acceptingProto
}

class TestHandler(implicit ec: ExecutionContext) extends BaseHandler {
  override val route: Route = {
    get {
      complete(StatusCodes.OK)
    }
  }
}
