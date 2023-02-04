package ru.auto.salesman.dao.testkit

import org.joda.time.DateTime
import ru.auto.salesman.dao.impl.jdbc.DateTimeResult
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.dao.slick.invariant.StaticQuery

class DatabaseFunctionsDao(database: Database) {

  def now(): DateTime = database.withSession { implicit session =>
    StaticQuery.queryNA[DateTime]("SELECT NOW()").first
  }
}
