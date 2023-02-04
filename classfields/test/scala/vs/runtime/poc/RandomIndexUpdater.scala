package vs.runtime.poc

import bootstrap.tracing.$
import vs.core.s3.IndexLoader
import zio.{Random, Schedule, Scope, ZIO, durationInt}

import scala.annotation.nowarn

/** Имитация обновления индекса: раз в `updateTime` обновляет значение одного
  * ключа или добавляет новое значение. Для тестов.
  */
object RandomIndexUpdater {

  val updateTime = 100.milliseconds

  @nowarn
  def update(
    key: String,
  ): ZIO[Random & IndexLoader & Scope & $, Throwable, Unit] = {
    (
      for {
        indexLoader <- ZIO.service[IndexLoader]
        rand        <- ZIO.service[Random]
        cur <- indexLoader
          .download(key)
          .flatMap { response =>
            response
              .stream
              .toInputStream
              .flatMap(is => PoCIndex.deserialize(is))
          }
        updated <-
          rand
            .nextIntBounded(10)
            .zipWith(rand.nextPrintableChar.map(_.toString))((i, s) =>
              cur.copy(map = cur.map.+((i, s))),
            )
        _ <- indexLoader.upload(key, Map.empty, updated.serializeToStream())
//        _ <- log.info(s"new random state: $updated")

      } yield ()
    ).repeat(Schedule.spaced(updateTime)).ignore

  }

}
