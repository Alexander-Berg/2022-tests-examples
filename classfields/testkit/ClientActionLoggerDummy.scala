package ru.auto.common.broker.testkit

import ru.auto.common.broker.ClientActionLogger
import ru.auto.common.broker.ClientActionLogger.ClientActionLogger
import ru.auto.common.context.RequestPayload
import ru.auto.log.client_action_log.{ActionPayload, ActionType}
import zio._

class ClientActionLoggerDummy(
    data: Ref[Chunk[(ActionType, ActionPayload, Option[RequestPayload])]])
  extends ClientActionLogger.Service {

  override def sendNoWait(payload: ActionPayload, actionType: ActionType, source: String): UIO[Unit] =
    sendNoWait(payload, actionType, Some(RequestPayload.empty.copy(deviceType = Some(source))))

  override def sendNoWait(
      payload: ActionPayload,
      actionType: ActionType,
      requestPayload: Option[RequestPayload]): UIO[Unit] =
    data.update(chunk => chunk.appended((actionType, payload, requestPayload)))

  def dump(): UIO[List[(ActionType, ActionPayload, Option[RequestPayload])]] = data.get.map(_.toList)
}

object ClientActionLoggerDummy {

  val test: ULayer[ClientActionLogger] =
    (for {
      ref <- Ref.make(Chunk[(ActionType, ActionPayload, Option[RequestPayload])]())
    } yield new ClientActionLoggerDummy(ref)).toLayer

}
