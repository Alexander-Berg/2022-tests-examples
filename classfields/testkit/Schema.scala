package ru.auto.comeback.storage

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
      _ <- sql"DELETE FROM comebacks".update.run.transact(xa).orDie
      _ <- sql"DELETE FROM comeback_updates".update.run.transact(xa).orDie
      _ <- sql"DELETE FROM search_subscriptions".update.run.transact(xa).orDie
      _ <- sql"DELETE FROM sender_log_events".update.run.transact(xa).orDie
    } yield ()
}
