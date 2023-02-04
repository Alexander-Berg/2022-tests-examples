package ru.yandex.realty.giraffic.service.mock

import ru.yandex.realty.canonical.base.request.Request
import ru.yandex.realty.giraffic.service.CountManager
import ru.yandex.realty.tracing.Traced
import zio.{Has, RIO, Task}

class TestOfferCountManager(resMap: Map[Request, Int]) extends CountManager.Service {

  override def getCountsMap(requests: Seq[Request]): RIO[Has[Traced], Map[String, Int]] =
    Task {
      requests.map { req =>
        req.key -> resMap.getOrElse(req, throw new IllegalArgumentException(s"Bad offers count spec for $req"))
      }.toMap
    }
}
