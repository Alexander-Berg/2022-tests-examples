package ru.yandex.vertis.moisha.impl.autoru.example

import akka.http.scaladsl.marshalling.ToRequestMarshaller
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller

import scala.concurrent.Future

/**
  * Async Moisha client that operates with domain objects.
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AsyncMoishaClient {
  def estimate[Req: ToRequestMarshaller, Res: FromResponseUnmarshaller](request: Req): Future[Res]
}
