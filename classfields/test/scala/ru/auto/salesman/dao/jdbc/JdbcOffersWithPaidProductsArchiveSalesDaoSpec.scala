package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.JdbcOffersWithPaidProductsArchiveSalesDao
import ru.auto.salesman.dao.{
  OffersWithPaidProductsArchiveSalesDao,
  OffersWithPaidProductsArchiveSalesDaoSpec
}
import ru.auto.salesman.test.template.SalesJdbcSpecTemplate

class JdbcOffersWithPaidProductsArchiveSalesDaoSpec
    extends OffersWithPaidProductsArchiveSalesDaoSpec
    with SalesJdbcSpecTemplate {

  override protected def dao: OffersWithPaidProductsArchiveSalesDao =
    new JdbcOffersWithPaidProductsArchiveSalesDao(database)
}
