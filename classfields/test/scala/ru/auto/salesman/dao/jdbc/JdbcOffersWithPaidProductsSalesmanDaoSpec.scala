package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.{
  OffersWithPaidProductsSalesmanDao,
  OffersWithPaidProductsSalesmanDaoSpec
}
import ru.auto.salesman.dao.OffersWithPaidProductsSalesmanDaoSpec.TestOffersWithPaidProductsSalesmanDao
import ru.auto.salesman.dao.impl.jdbc.JdbcOffersWithPaidProductsSalesmanDao
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate

import scala.util.Try

class JdbcOffersWithPaidProductsSalesmanDaoSpec
    extends OffersWithPaidProductsSalesmanDaoSpec
    with SalesmanJdbcSpecTemplate {

  protected val dao: TestOffersWithPaidProductsSalesmanDao =
    new JdbcOffersWithPaidProductsSalesmanDao(database)
      with TestOffersWithPaidProductsSalesmanDao {

      def insert(
          items: Iterable[
            OffersWithPaidProductsSalesmanDao.OfferWithPaidProduct
          ]
      ): Try[Unit] =
        Try {
          database.withSession { implicit session =>
            JdbcOffersWithPaidProductsDao.insertQuery(items).executeBatch()
          }
        }
    }
}
