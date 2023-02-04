package ru.yandex.vertis.billing.emon.scheduler

import billing.emon.model.{EventId, EventState}
import ru.yandex.vertis.billing.emon.scheduler.EventStateBuilder.EventStateBuilder
import ru.yandex.vertis.ydb.zio.{TxError, TxUIO}
import zio._
import zio.test.mock._

object EventStateBuilderMock extends Mock[EventStateBuilder] {
  object BuildEventState extends Effect[(EventId, Long), TxError[Nothing], Option[EventState]]

  val compose: URLayer[Has[Proxy], EventStateBuilder] =
    ZLayer.fromServiceM { proxy: Proxy =>
      withRuntime.map { _ =>
        new EventStateBuilder.Service {
          def buildEventState(eventId: EventId, snapshotId: Long): TxUIO[Option[EventState]] =
            proxy(BuildEventState, eventId, snapshotId)
        }
      }
    }
}
