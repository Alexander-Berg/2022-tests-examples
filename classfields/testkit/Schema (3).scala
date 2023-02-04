package auto.dealers.balance_alerts.storage.testkit

import common.zio.doobie.schema
import doobie._
import doobie.implicits._
import zio._
import zio.interop.catz._

object Schema {

  val init: ZIO[Has[Transactor[Task]], Nothing, Unit] =
    ZIO.service[Transactor[Task]].flatMap { xa =>
      schema.InitSchema("/schema.sql", xa).orDie
    }

  val cleanup: ZIO[Has[Transactor[Task]], Nothing, Unit] =
    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- sql"DELETE FROM balance_events".update.run.transact(xa).orDie
      _ <- sql"DELETE FROM notifications".update.run.transact(xa).orDie
    } yield ()

}
