package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.{JdbcAdsRequestDao, JdbcClientsChangedBufferDao}
import ru.auto.salesman.dao.{AdsRequestDao, AdsRequestDaoSpec}
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate

class JdbcAdsRequestDaoSpec extends AdsRequestDaoSpec with SalesmanJdbcSpecTemplate {

  override val schemaScript: String = "/sql/salesman.final.sql"

  override protected val adsRequestDao: AdsRequestDao = new JdbcAdsRequestDao(
    database
  )

  override protected val clientChangedBufferDao =
    new JdbcClientsChangedBufferDao(database)
}
