package ru.yandex.vertis.parsing.util.dao.jooq

import org.jooq._
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockResult
import org.scalatest.FunSuiteLike

import scala.collection.mutable.ArrayBuffer

/**
  * TODO
  *
  * @author aborunov
  */
class MockedQueries extends FunSuiteLike {
  private val testData = ArrayBuffer[MockedQuery]()

  private var currentQueryNum: Int = 0

  private var queriesRequested: Int = 0
  private var queriesSuccessfullyCalled: Int = 0

  def nextQuery: MockedQuery = {
    queriesRequested += 1
    if (currentQueryNum >= testData.length) {
      if (testData.isEmpty) sys.error("No mocked queries left")
      else testData.last
    } else {
      try {
        testData(currentQueryNum)
      } finally {
        currentQueryNum += 1
      }
    }
  }

  def ensureNextSuccessCall(): Unit = {
    queriesSuccessfullyCalled += 1
  }

  def verifyAllExpectedQueriesCalled(): Unit = {
    assert(queriesRequested >= testData.length)
    assert(queriesSuccessfullyCalled == queriesRequested)
  }

  def clear(): Unit = {
    testData.clear()
    currentQueryNum = 0
    queriesSuccessfullyCalled = 0
    queriesRequested = 0
  }

  def expectBatchQuery(queries: Seq[String]): Unit = {
    val mockedQuery = MockedQuery()
    mockedQuery.expectedQuery.set((queries, Seq.empty))
    testData += mockedQuery
  }

  def expectQuery(query: String, bindings: Seq[AnyRef] = Seq.empty): Unit = {
    val mockedQuery = MockedQuery()
    mockedQuery.expectedQuery.set((Seq(query), bindings))
    testData += mockedQuery
  }

  def respondWithEmptyQueryResult(fields: Field[_]*): Unit = {
    respondWithQueryResult(fields: _*)()
  }

  def respondWithSingleQueryResult(fields: Field[_]*)(values: AnyRef*): Unit = {
    respondWithQueryResult(fields: _*)(values)
  }

  def respondWithQueryResult(fields: Field[_]*)(values: Seq[AnyRef]*): Unit = {
    val lastMockedQuery = testData.last
    val dsl = DSL.using(SQLDialect.MYSQL)
    val result = dsl.newResult(fields: _*)
    values.foreach(row => {
      val record = dsl.newRecord(fields: _*)
      record.fromArray(row: _*)
      result.add(record)
    })
    lastMockedQuery.providedResult.set(Seq(new MockResult(result.size(), result)))
  }

  def respondWithEmptyTableResult(table: Table[_]): Unit = {
    respondWithTableResult(table)()
  }

  def respondWithSingleTableResult(table: Table[_])(values: AnyRef*): Unit = {
    respondWithTableResult(table)(values)
  }

  def respondWithTableResult(table: Table[_])(values: Seq[AnyRef]*): Unit = {
    val lastMockedQuery = testData.last
    val dsl = DSL.using(SQLDialect.MYSQL)
    val result = dsl.newResult(table.fields(): _*)
    values.foreach(row => {
      val record = dsl.newRecord(table.fields(): _*)
      record.fromArray(row: _*)
      result.add(record)
    })
    lastMockedQuery.providedResult.set(Seq(new MockResult(result.size(), result)))
  }

  def respondWithUpdateResult(updateResult: Seq[Boolean]): Unit = {
    val lastMockedQuery = testData.last
    val result = updateResult.map(updated => new MockResult(if (updated) 1 else 0, null))
    lastMockedQuery.providedResult.set(result)
  }
}
