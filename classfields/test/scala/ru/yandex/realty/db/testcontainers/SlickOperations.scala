package ru.yandex.realty.db.testcontainers

import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.util.{Failure, Success}

trait SlickOperations {
  this: DatabaseProfile =>

  implicit class StringContextSqlOps(val sc: StringContext) {

    def script(args: Any*): DBIOAction[Seq[Int], NoStream, Effect] = {
      val api = jdbcProfile.api
      import api._
      val filename = sc.raw(args: _*)
      val action = for {
        statements <- SqlUtils.loadScript(filename)
        action = DBIO.sequence(statements.map(st => sqlu"#$st"))
      } yield action

      action match {
        case Success(value) => value
        case Failure(err) => DBIO.failed(new RuntimeException(s"Can't execute script s[$filename]", err))
      }
    }
  }
}
