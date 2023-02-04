package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.{JdbcCarsGoodsQueries, JdbcGoodsDao}
import ru.auto.salesman.dao.{GoodsDao, GoodsDaoSpec}
import ru.auto.salesman.test.template.SalesJdbcSpecTemplate

class JdbcGoodsDaoSpec extends GoodsDaoSpec with SalesJdbcSpecTemplate {

  val goodsDao: GoodsDao = new JdbcGoodsDao(JdbcCarsGoodsQueries, database)
}
