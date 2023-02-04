package ru.auto.salesman.dao.impl.jdbc.test

import ru.auto.salesman.dao.impl.jdbc.JdbcAutostrategiesDao

import scala.slick.jdbc.StaticQuery
import ru.auto.salesman.dao.impl.jdbc.database.Database

class JdbcAutostrategiesDaoForTests(database: Database)
    extends JdbcAutostrategiesDao(database) {

  def clean(): Unit =
    database.withSession { implicit session =>
      StaticQuery.queryNA[Int]("DELETE FROM autostrategies").execute
    }
}
