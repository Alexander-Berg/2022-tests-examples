package auto.dealers.trade_in_notifier.storage.testkit

import zio._
import zio.interop.catz._
import zio.blocking.Blocking
import doobie.Transactor
import doobie.implicits._
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql

object Schema {

  val live: ZLayer[Blocking, Nothing, Has[Transactor[Task]]] =
    TestPostgresql.managedTransactor

  val dbInit: URIO[Has[Transactor[Task]], Unit] = ZIO
    .service[Transactor[Task]]
    .flatMap(InitSchema("/schema.sql", _))
    .orDie

  val dbCleanup: URIO[Has[Transactor[Task]], Unit] = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"DELETE FROM trade_in_notification_buffer".update.run.transact(xa) *>
        sql"DELETE FROM trade_in_notification_log".update.run.transact(xa) *>
        sql"DELETE FROM trade_in_offer_log".update.run.transact(xa) *>
        ZIO.unit
    }
    .orDie

}
