package ru.auto.salesman.dao.jdbc

import org.scalatest.BeforeAndAfterAll
import ru.auto.salesman.dao.impl.jdbc.{JdbcCashbackPeriodDao, JdbcClientsChangedBufferDao}
import ru.auto.salesman.dao.{
  CashbackPeriodDao,
  CashbackPeriodDaoSpec,
  ClientsChangedBufferDao
}
import ru.auto.salesman.test.template.SalesmanCashbackPeriodJdbcSpecTemplate

class JdbcCashbackPeriodDaoSpec
    extends CashbackPeriodDaoSpec
    with SalesmanCashbackPeriodJdbcSpecTemplate
    with BeforeAndAfterAll {

  def cashbackPeriodDao: CashbackPeriodDao =
    new JdbcCashbackPeriodDao(database)

  override def afterAll: Unit =
    database.withSession { implicit session =>
      session.conn.prepareStatement("DELETE FROM cashback_periods").execute
    }

  override def clientsChangedBufferDao: ClientsChangedBufferDao =
    new JdbcClientsChangedBufferDao(database)
}
