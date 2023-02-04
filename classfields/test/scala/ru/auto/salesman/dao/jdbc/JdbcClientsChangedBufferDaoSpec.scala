package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.JdbcClientsChangedBufferDao
import ru.auto.salesman.dao.{ClientsChangedBufferDao, ClientsChangedBufferDaoSpec}
import ru.auto.salesman.test.template.ClientsChangedBufferJdbcSpecTemplate

class JdbcClientsChangedBufferDaoSpec
    extends ClientsChangedBufferDaoSpec
    with ClientsChangedBufferJdbcSpecTemplate {

  def clientsChangedBufferDao: ClientsChangedBufferDao =
    new JdbcClientsChangedBufferDao(database)
}
