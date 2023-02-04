package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.{JdbcClientsChangedBufferDao, JdbcQuotaRequestDao}
import ru.auto.salesman.dao.impl.jdbc.JdbcQuotaRequestDao._
import ru.auto.salesman.dao.{
  ClientsChangedBufferDao,
  QuotaRequestDao,
  QuotaRequestDaoSpec
}
import ru.auto.salesman.model.QuotaEntities
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate

class JdbcQuotaRequestDaoSpec extends QuotaRequestDaoSpec with SalesmanJdbcSpecTemplate {

  override protected val dao: QuotaRequestDao = new JdbcQuotaRequestDao(
    database
  )

  override protected val clientChangedBufferDao: ClientsChangedBufferDao =
    new JdbcClientsChangedBufferDao(database)

  override protected def cleanTables(): Unit = {
    val connection = database.createConnection()
    for (quotaEntity <- QuotaEntities.values)
      connection
        .createStatement()
        .executeUpdate(s"DELETE FROM ${tableName(quotaEntity)}")
    connection.close()
  }
}
