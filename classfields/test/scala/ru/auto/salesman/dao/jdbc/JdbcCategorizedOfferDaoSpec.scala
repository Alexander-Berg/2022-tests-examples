package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.{JdbcCategorizedOfferDao, JdbcOfferDao}
import ru.auto.salesman.dao.{CategorizedOfferDaoSpec, OfferDao}
import ru.auto.salesman.test.template.CategorizedSalesJdbcSpecTemplate

class JdbcCategorizedOfferDaoSpec
    extends CategorizedOfferDaoSpec
    with CategorizedSalesJdbcSpecTemplate {

  val offerDao: OfferDao = new JdbcOfferDao(JdbcCategorizedOfferDao, database)
}
