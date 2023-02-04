package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.OffersWithPaidProductsSalesDaoSpec
import ru.auto.salesman.dao.impl.JdbcOffersWithPaidProductsSalesDao
import ru.auto.salesman.test.template.SalesJdbcSpecTemplate

class JdbcOffersWithPaidProductsSalesDaoSpec
    extends OffersWithPaidProductsSalesDaoSpec
    with SalesJdbcSpecTemplate {

  protected val dao = new JdbcOffersWithPaidProductsSalesDao(database)
}
