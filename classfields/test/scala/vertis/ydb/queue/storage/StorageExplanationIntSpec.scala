package vertis.ydb.queue.storage

import vertis.ydb.queue.QueueStorageTest
import vertis.ydb.queue.storage.queries._
import vertis.zio.test.ZioSpecBase

/** To manually check ydb execution plans
  * Could be run as a test to check all queries compile
  * Could be an alert for the word 'FullScan' appearing
  */
class StorageExplanationIntSpec extends ZioSpecBase with QueueStorageTest {

  private val tableName = "queue"
  private val fWord = "FullScan"

  "StorageExplanation" should {

    "explain queries and do some performance check" in withExplainResult(
      new AddElementQuery[Unit](storage.columnNames, tableName),
      new AddElementsBatchQuery[Unit](storage.columnNames, tableName),
      new CountElementsQuery(storage.columnNames, tableName),
      new DeleteTopElementsQuery(storage.columnNames, tableName),
      new PeekElementsQuery(storage.columnNames, tableName),
      new PollElementsQuery(storage.columnNames, tableName)
    ) { result =>
      val plan = result.getQueryPlan
//      val ast = result.getQueryAst
      plan should not contain fWord
//      ast should not contain fWord
    }

  }
}
