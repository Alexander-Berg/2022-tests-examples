package ru.auto.salesman.dao

import org.joda.time.DateTime
import ru.auto.salesman.dao.BalanceOrderDao.{ForProductCreatedSince, Record}
import ru.auto.salesman.test.BaseSpec

import scala.util.Success

trait BalanceOrderDaoSpec extends BaseSpec {

  def balanceOrderDao: BalanceOrderDao

  "BalanceOrderDao" should {

    "find orders by product and created since" in {
      val orders = balanceOrderDao.get(
        ForProductCreatedSince(
          productId = 175L,
          createDateFrom = new DateTime("2015-08-06T17:11:00.000+03:00").getMillis
        )
      )

      orders shouldEqual Success(
        Iterable(
          Record(
            id = 15581L,
            clientId = 6838000L,
            agencyId = None,
            createDate = new DateTime("2015-08-06T17:12:00.000+03:00")
          )
        )
      )
    }
  }
}
