package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.{JdbcCategorizedGoodsQueries, JdbcGoodsDao}
import ru.auto.salesman.dao.{CategorizedGoodsDaoSpec, GoodsDao}
import ru.auto.salesman.test.template.SalesJdbcSpecTemplate

class JdbcCategorizedGoodsDaoSpec
    extends CategorizedGoodsDaoSpec
    with SalesJdbcSpecTemplate {

  val goodsDao: GoodsDao = new JdbcGoodsDao(JdbcCategorizedGoodsQueries, database)
}
