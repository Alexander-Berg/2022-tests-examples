package ru.yandex.realty.giraffic.service.mock

import ru.yandex.realty.canonical.base.request.Request
import ru.yandex.realty.traffic.service.FrontendRouter
import ru.yandex.realty.urls.router.model.{RouterUrlRequest, RouterUrlResponse, ViewType}
import zio._

import scala.util.Try

class TestFrontendRouter(
  parseMap: Map[String, Request],
  translateMap: Map[Request, Option[String]]
) extends FrontendRouter.Service {

  override def parse(urlPath: String, viewType: ViewType): Task[Try[Request]] =
    if (parseMap.contains(urlPath)) Task.succeed(Try(parseMap(urlPath)))
    else throw new IllegalArgumentException(s"Bad parse spec for $urlPath")

  override def translate(requests: Iterable[RouterUrlRequest]): Task[Iterable[RouterUrlResponse]] =
    Task {
      requests.map { r =>
        RouterUrlResponse(
          r,
          translateMap.getOrElse(r.req, throw new RuntimeException(s"Bad translate spec for ${r.req}"))
        )
      }
    }
}
