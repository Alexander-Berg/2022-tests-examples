package ru.yandex.vertis.parsing.util.dao.jooq

import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.{DSLContext, SQLDialect}

/**
  * TODO
  *
  * @author aborunov
  */
class TestDslContextProvider(testData: MockedQueries) {
  val testDataProvider = new TestDataProvider(testData)

  def dsl: DSLContext = {
    val connection = new MockConnection(testDataProvider)
    DSL.using(connection, SQLDialect.MYSQL)
  }
}
