package ru.yandex.vertis.parsing.util.dao

import org.apache.commons.dbcp2.BasicDataSource
import org.jooq._
import ru.yandex.vertis.parsing.components.query.{QueryResult, SimpleQueryExecutorSupport}
import ru.yandex.vertis.parsing.util.dao.jdbc.JdbcWrapperImpl
import ru.yandex.vertis.parsing.util.dao.jooq.dsl.TestDSLContextSupport
import ru.yandex.vertis.parsing.util.dao.jooq.{JooqWrapper, JooqWrapperImpl, MockedQueries}
import ru.yandex.vertis.tracing.Traced

import scala.util.Try

/**
  * TODO
  *
  * @author aborunov
  */
class TestDatabase extends Database {

  override def jdbc: JdbcWrapperImpl = {
    sys.error("not implemented for TestDatabase")
  }

  private val testData = new MockedQueries

  def expectBatchQuery(queries: Seq[String]): Unit = {
    testData.expectBatchQuery(queries)
  }

  def clear(): Unit = {
    testData.clear()
  }

  def expectQuery(query: String, bindings: Seq[AnyRef] = Seq.empty): Unit = {
    testData.expectQuery(query, bindings)
  }

  def verifyAllExpectedQueriesCalled(): Unit = {
    testData.verifyAllExpectedQueriesCalled()
  }

  def respondWithEmptyQueryResult(fields: Field[_]*): Unit = {
    testData.respondWithEmptyQueryResult(fields: _*)
  }

  def respondWithSingleQueryResult(fields: Field[_]*)(values: AnyRef*): Unit = {
    testData.respondWithSingleQueryResult(fields: _*)(values: _*)
  }

  def respondWithQueryResult(fields: Field[_]*)(values: Seq[AnyRef]*): Unit = {
    testData.respondWithQueryResult(fields: _*)(values: _*)
  }

  def respondWithEmptyTableResult(table: Table[_]): Unit = {
    testData.respondWithEmptyTableResult(table)
  }

  def respondWithSingleTableResult(table: Table[_])(values: AnyRef*): Unit = {
    testData.respondWithSingleTableResult(table)(values: _*)
  }

  def respondWithTableResult(table: Table[_])(values: Seq[AnyRef]*): Unit = {
    testData.respondWithTableResult(table)(values: _*)
  }

  def respondWithUpdateResult(updateResult: Seq[Boolean]): Unit = {
    testData.respondWithUpdateResult(updateResult)
  }

  override val jooq: JooqWrapper = new JooqWrapperImpl with TestDSLContextSupport with SimpleQueryExecutorSupport {
    override def testData: MockedQueries = TestDatabase.this.testData

    override def dataSource: BasicDataSource = {
      sys.error("not implemented for TestDatabase")
    }

    override def queryLazy[T <: Record, X](
        name: String
    )(f: DSLContext => SelectOptionStep[T])(process: Try[QueryResult] => X)(implicit trace: Traced): X = {
      sys.error("not implemented for TestDatabase")
    }
  }

  override def withTransaction[R](action: => R): R = action

  override def withTransactionOfLevel[R](level: Int)(action: => R): R = action

  override def withTransactionReadCommitted[R](action: => R): R = action
}
