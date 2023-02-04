package ru.yandex.vertis.parsing.util.dao.jooq.dsl

import org.jooq.DSLContext
import ru.yandex.vertis.parsing.util.dao.jooq.{MockedQueries, MockedQuery, TestDslContextProvider}

/**
  * TODO
  *
  * @author aborunov
  */
trait TestDSLContextSupport extends DSLContextAware {
  def testData: MockedQueries

  val dslContextProvider = new TestDslContextProvider(testData)

  def dsl: DSLContext = dslContextProvider.dsl
}
