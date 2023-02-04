package ru.yandex.realty.db

import slick.basic.BasicProfile
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.Future

trait DbSpecBase {
  type DbProfile <: BasicProfile
  type Db = DbProfile#Backend#Database
  implicit def database: Db

  implicit class DBIOActionOps[+R, +S <: NoStream, -E <: Effect](action: DBIOAction[R, S, E]) {
    def databaseValue: Future[R] = database.run(action)
  }
}
