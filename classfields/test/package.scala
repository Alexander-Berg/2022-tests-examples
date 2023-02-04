package vsmoney.auction_auto_strategy.storage

import common.zio.doobie.schema._
import doobie._
import doobie.implicits._
import zio._
import zio.interop.catz._

package object test {

  private[test] val init =
    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- InitSchema("/resources/schema.sql", xa)
    } yield ()

  private[test] def truncate(table: Fragment) =
    ZIO
      .service[Transactor[Task]]
      .flatMap(sql"TRUNCATE TABLE $table".update.run.transact(_).unit)

}
