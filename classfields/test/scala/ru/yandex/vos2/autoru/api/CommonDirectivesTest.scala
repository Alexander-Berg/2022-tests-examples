package ru.yandex.vos2.autoru.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsArray, JsObject, Json}
import ru.yandex.vertis.application.deploy.Deploys
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.{RuntimeConfig, RuntimeConfigImpl}
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, TracingSupport}
import ru.yandex.vos2.api.tracing.{RequestDirectives, TracingDirectives}
import ru.yandex.vos2.api.utils.{DomainExceptionHandler, DomainRejectionHandler, ErrorCode}
import ru.yandex.vos2.autoru.api.utils.CommonDirectives._
import ru.yandex.vos2.util.VosPlayJsonSupport._

/**
  * Created by andrey on 12/9/16.
  */
@RunWith(classOf[JUnitRunner])
class CommonDirectivesTest
  extends AnyFunSuiteLike
  with Matchers
  with ScalatestRouteTest
  with OptionValues
  with DomainExceptionHandler
  with TracingDirectives
  with DomainRejectionHandler {

  implicit protected val td = TildeArrow.injectIntoRoute

  private val tracingSupport: TracingSupport = {
    val endpointConfig = EndpointConfig("component", "localhost", 36240)
    LocalTracingSupport(endpointConfig)
  }

  private val runtimeConfig: RuntimeConfig =
    RuntimeConfigImpl(Environments.Testing, "localhost", "localhost", Deploys.Debian, None)

  private def innerTrace: Directive0 = {
    trace(tracingSupport, runtimeConfig)
  }

  private def sealRoute(route: Route) = {
    extractRequestContext { rc =>
      Route.seal(route)(rc.settings, rc.parserSettings, specificRejectionHandler, specificExceptionHandler)
    }
  }

  val route: Route = RequestDirectives.wrapRequest {
    innerTrace {
      get {
        pathEndOrSingleSlash {
          extractRequestInfo { requestInfo =>
            mapResponseHeaders { headers =>
              RawHeader("X-Request-Id", requestInfo.ctx.requestId) +: headers
            } { http =>
              http.complete(requestInfo.toString)
            }
          }
        } ~ path("boolTest") {
          (bool("b1") & optBool("b2")) { (b1, b2) => request =>
            request.complete(b1.toString + " " + b2.toString)
          }
        } ~ path("filtersTest") {
          filtersDirective { filters => request =>
            request.complete(filters.toString)
          }
        }
      }
    }
  }

  test("filtersTest: unknown service") {
    Get("/filtersTest?service=xxx") ~> sealRoute(route) ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[JsObject] shouldBe renderError(
        ErrorCode.IllegalArgumentCode,
        "Failed to parse filters: key not found: xxx"
      )
    }
  }

  test("boolTest") {
    Get("/boolTest?b1=1") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "true None"
    }
    Get("/boolTest?b1=true") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "true None"
    }
    Get("/boolTest?b1=true&b2=true") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "true Some(true)"
    }
    Get("/boolTest?b1=0&b2=false") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "false Some(false)"
    }
    Get("/boolTest?b1=bla&b2=true") ~> sealRoute(route) ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[JsObject].value
        .get("errors")
        .get
        .as[JsArray]
        .value(0)
        .as[JsObject]
        .value
        .get("description")
        .get
        .as[String] shouldBe "wrong boolean param: expected to be 1 or 0 or true or false"
    }
  }

  test("requestInfo") {
    Get() ~> addHeaders(
      RawHeader("X-UserRegionId", "213"),
      RawHeader("X-UserIp", "fe80::f6f9:51ff:fef1:3fda"),
      RawHeader("X-UserId", "221411"),
      RawHeader("X-AutoruUID", "autoru123"),
      RawHeader("X-UserAgent", "xxx"),
      RawHeader("X-User-Location", "lat=5;lon=6;acc=7")
    ) ~> route ~> check {
      val traceId = header("x-request-id").value.value
      val response: String = responseAs[String]
      withClue(response) {
        response shouldBe "RequestInfo(regionId=Some(213), userIp=Some(fe80::f6f9:51ff:fef1:3fda), " +
          "userId=Some(221411), autoruUid=Some(autoru123), userAgent=Some(xxx), " +
          s"userLocation=Some(lat=5.0;lon=6.0;acc=7.0), trace=$traceId, platform=None, isCallCenter=false)"
      }
    }
  }

  test("empty requestInfo") {
    Get() ~> route ~> check {
      val response: String = responseAs[String]
      val traceId = header("x-request-id").value.value
      withClue(response) {
        response shouldBe "RequestInfo(regionId=None, userIp=None, userId=None, autoruUid=None, userAgent=None, " +
          s"userLocation=None, trace=$traceId, platform=None, isCallCenter=false)"
      }
    }
  }

  test("nonempty request id") {
    Get() ~> addHeaders(
      RawHeader("x-request-id", "35a332ab2d65e183f6ec3a1b0688672d")
    ) ~> route ~> check {
      val response: String = responseAs[String]
      withClue(response) {
        response shouldBe "RequestInfo(regionId=None, userIp=None, userId=None, autoruUid=None, userAgent=None, " +
          "userLocation=None, trace=35a332ab2d65e183f6ec3a1b0688672d, platform=None, isCallCenter=false)"
      }
    }
  }

  test("user location other variants") {
    Get() ~> addHeaders(
      RawHeader("x-request-id", "35a332ab2d65e183f6ec3a1b0688672d"),
      RawHeader("X-User-Location", "  lon = 6 ; lat  = 5 ;  acc =   7  ")
    ) ~> route ~> check {
      val response: String = responseAs[String]
      withClue(response) {
        response shouldBe "RequestInfo(regionId=None, userIp=None, userId=None, autoruUid=None, userAgent=None, " +
          "userLocation=Some(lat=5.0;lon=6.0;acc=7.0), trace=35a332ab2d65e183f6ec3a1b0688672d, platform=None, isCallCenter=false)"
      }
    }
  }
}
