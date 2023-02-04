package ru.yandex.vertis.billing.internal_api.routes.v1.service.resolution

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.joda.time.DateTime
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.internal_api.RootHandlerSpecBase
import ru.yandex.vertis.billing.model_core.Resolution._
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{teleponyCallFactGen, Producer, ResolutionsVectorGen}
import ru.yandex.vertis.billing.internal_api.routes.v1.service.resolution.Handler
import ru.yandex.vertis.billing.internal_api.routes.v1.view.ResolutionView
import ru.yandex.vertis.billing.service.CallsResolutionService
import ru.yandex.vertis.billing.service.CallsResolutionService.{ByIds, InInterval, Patch}
import ru.yandex.vertis.billing.util.DateTimeUtils.{now, IsoDateFormatter}
import ru.yandex.vertis.billing.util.{DateTimeInterval, RequestContext}
import ru.yandex.vertis.hobo.proto.Model.SuspiciousCallResolution
import ru.yandex.vertis.hobo.proto.Model.{Resolution => HoboResolution, SuspiciousCallResolution}
import spray.json._

import scala.concurrent.Future

/**
  * Spec on [[Handler]]
  *
  * @author ruslansd
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/calls-resolution"

  private val callFact = teleponyCallFactGen().next
  private val header = CallFactHeader(callFact)
  private val callId = header.identity
  private val resolution = Manually(Statuses.Pass)

  private val evaluatedCalls =
    (1 to 5).map(_ => EvaluatedCallFact(teleponyCallFactGen().next, ResolutionsVectorGen.next, DateTime.now))

  "POST /{id}/status" should {
    "set status" in {
      val content = ResolutionView(resolution).toJson.compactPrint
      val patch = new Patch(resolution = Some(resolution))
      stub(backend.callResolutionService.get.update(_: CallFactId, _: Patch)(_: RequestContext)) {
        case (`callId`, `patch`, `operator`) =>
          Future.successful(())
      }
      Post(url(s"/${header.identity}/status"), HttpEntity(ContentTypes.`application/json`, content)) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "GET /{id}" should {
    "provide call and resolution by id" in {
      val ecf = EvaluatedCallFact(callFact, ResolutionsVector(), now())
      stub(backend.callResolutionService.get.get(_: CallsResolutionService.Filter)(_: RequestContext)) {
        case (ByIds(_), `operator`) =>
          Future.successful(Iterable(ecf))
      }
      Get(url(s"/${header.identity}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "GET /" should {
    val isoDate = DateTime.now
    val date = IsoDateFormatter.print(isoDate)
    val interval = DateTimeInterval.dayIntervalFrom(isoDate.withTimeAtStartOfDay())
    val request = url(s"/?date=$date")
    val filter = InInterval(interval)
    "provide call and resolutions for specified time" in {
      stub(backend.callResolutionService.get.get(_: CallsResolutionService.Filter)(_: RequestContext)) {
        case (`filter`, `operator`) =>
          Future.successful(evaluatedCalls)
      }
      Get(request) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "provide call without operator context" in {
      stub(backend.callResolutionService.get.get(_: CallsResolutionService.Filter)(_: RequestContext)) {
        case (`filter`, _) =>
          Future.successful(evaluatedCalls)
      }
      Get(request) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }
  }

  private val hoboResolution =
    HoboResolution
      .newBuilder()
      .setVersion(1)
      .setSuspiciousCall(
        SuspiciousCallResolution
          .newBuilder()
          .setVersion(1)
          .setComment("Super Suspicious call")
          .setValue(SuspiciousCallResolution.Value.BAD_PHONE_NUMBER)
      )
      .build()

  "GET /{id}" should {
    val filter = CallsResolutionService.ByIds(header.identity)
    val evaluatedCallFact =
      Iterable(EvaluatedCallFact(callFact, ResolutionsVector(), now(), hoboResolution = Some(hoboResolution)))
    "provide call and resolution by id with hobo resolution" in {
      stub(backend.callResolutionService.get.get(_: CallsResolutionService.Filter)(_: RequestContext)) {
        case (`filter`, _) =>
          Future.successful(evaluatedCallFact)
      }
      Get(url(s"/${header.identity}")) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }
  }
}
