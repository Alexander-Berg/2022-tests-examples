package ru.yandex.vertis.parsing.util.dao.jooq

import org.jooq.tools.jdbc.{MockDataProvider, MockExecuteContext, MockResult}
import org.scalatest.FunSuiteLike

/**
  * TODO
  *
  * @author aborunov
  */
class TestDataProvider(testData: MockedQueries) extends MockDataProvider with FunSuiteLike {

  override def execute(ctx: MockExecuteContext): Array[MockResult] = {
    val mockedQuery = testData.nextQuery
    if (!ctx.batch()) {
      assert(ctx.sql() == mockedQuery.expectedQuery.get()._1.head)
      assert(ctx.bindings().toSeq == mockedQuery.expectedQuery.get()._2)
    } else {
      assert(ctx.batchSQL().toSeq == mockedQuery.expectedQuery.get()._1)
      assert(ctx.bindings().isEmpty)
    }
    testData.ensureNextSuccessCall()
    mockedQuery.providedResult.get().toArray
  }
}
