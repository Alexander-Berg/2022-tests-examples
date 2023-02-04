package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.JdbcOfferDao
import ru.auto.salesman.dao.{OfferDao, OfferDaoSpec}
import ru.auto.salesman.test.template.SalesJdbcSpecTemplate

class JdbcOfferDaoSpec extends OfferDaoSpec with SalesJdbcSpecTemplate {
  val offerDao: OfferDao = new JdbcOfferDao(JdbcOfferDao, database)
}
