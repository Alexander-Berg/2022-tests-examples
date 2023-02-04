package ru.yandex.vertis.telepony

import ru.yandex.vertis.telepony.util.db.PlainDualDatabase
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.Future

/**
  * @author @logab
  */
trait DatabaseSpec {

  implicit def database: PlainDualDatabase

  implicit class DBIOActionOps[+R, +S <: NoStream, -E <: Effect](action: DBIOAction[R, S, E]) {
    def databaseValue: Future[R] = database.master.db.run(action)
  }
}
