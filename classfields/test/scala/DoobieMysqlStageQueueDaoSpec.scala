import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, TestContainerDatasource}
import ru.yandex.realty.doobie.{DoobieTestDatabase, StubDbMonitorFactory}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching2.StageQueueDao.QueueItem
import ru.yandex.realty.watching2.doobie.DoobieMysqlStageQueueDao

import java.time.Instant

@RunWith(classOf[JUnitRunner])
class DoobieMysqlStageQueueDaoSpec
  extends WordSpecLike
  with MySQLTestContainer.V8_0
  with TestContainerDatasource
  with BeforeAndAfterAll
  with ScalaFutures
  with Matchers
  with DoobieTestDatabase {

  implicit val trace: Traced = Traced.empty

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(20, Millis))
  private val dao = new DoobieMysqlStageQueueDao(new StubDbMonitorFactory)
  private val DefaultEntityType = "entity_type_1"

  override def beforeAll() = {
    doobieDatabase.masterTransaction { implicit t =>
      executeSqlScript("sql/stage_queue_schema.sql")
    }(Traced.empty).futureValue
  }

  private def queueItem(
    stageId: String,
    entityId: String,
    version: Long,
    shardId: Int,
    visitTime: Int,
    entityType: String = DefaultEntityType
  ): QueueItem =
    QueueItem(entityType, stageId, entityId, version, shardId, createTimestamp(visitTime))

  private def createTimestamp(millisShift: Int) = Instant.ofEpochSecond(1).plusMillis(millisShift)

  "DoobieMysqlStageQueueDao" should {
    "insert new items in queue" in {
      val stageName = "stage1"
      val shardId = 1
      val batch = List(
        queueItem(stageName, "a", 0, shardId, 0),
        queueItem(stageName, "b", 1, shardId, 1)
      )
      doobieDatabase.masterTransaction(dao.offer(batch)(_)).futureValue
      val peekResult = doobieDatabase
        .masterTransaction(
          dao.peek(List(shardId), DefaultEntityType, stageName, 10, createTimestamp(2))(_)
        )
        .futureValue
      peekResult.toSet shouldBe batch.toSet
    }

    "save the minimum" in {
      val stageName = "stage2"
      val shardId = 1
      val batch1 = List(
        queueItem(stageName, "a", 1, shardId, 5),
        queueItem(stageName, "b", 2, shardId, 5),
        queueItem(stageName, "c", 3, shardId, 5),
        queueItem(stageName, "d", 4, shardId, 5)
      )
      val batch2 = List(
        queueItem(stageName, "a", 4, shardId, 2), // should overwrite visitTime
        queueItem(stageName, "b", 5, shardId, 7), // visitTime should be ignored
        //
        queueItem(stageName, "c", 6, shardId, 2),
        queueItem(stageName, "c", 7, shardId, 3), // the same key as in the previous element, visitTime should be ignored
        //
        queueItem(stageName, "d", 8, shardId, 3),
        queueItem(stageName, "d", 9, shardId, 2), // the same key as in the previous element, should overwrite visitTime
        //
        queueItem(stageName, "e", 10, shardId, 9) // new element
      )
      doobieDatabase.masterTransaction(dao.offer(batch1)(_)).futureValue
      doobieDatabase.masterTransaction(dao.offer(batch2)(_)).futureValue
      val peekResult = doobieDatabase
        .masterTransaction(
          dao.peek(List(shardId), DefaultEntityType, stageName, 10, createTimestamp(100))(_)
        )
        .futureValue
      peekResult.toSet shouldBe Set(
        queueItem(stageName, "a", 4, shardId, 2),
        queueItem(stageName, "b", 5, shardId, 5),
        queueItem(stageName, "c", 7, shardId, 2),
        queueItem(stageName, "d", 9, shardId, 2),
        queueItem(stageName, "e", 10, shardId, 9)
      )
    }

    "peek items less than or equal to timestamp sorted by visit time" in {
      val stageName = "stage3"
      val shardId = 3
      val batch = List(
        queueItem(stageName, "a", 1, shardId, 2),
        queueItem(stageName, "b", 2, shardId, 1),
        queueItem(stageName, "c", 3, shardId, 3)
      )
      doobieDatabase.masterTransaction(dao.offer(batch)(_)).futureValue
      val peekResult = doobieDatabase
        .masterTransaction(
          dao.peek(List(shardId), DefaultEntityType, stageName, 10, createTimestamp(2))(_)
        )
        .futureValue
      peekResult shouldBe List(
        queueItem(stageName, "b", 2, shardId, 1),
        queueItem(stageName, "a", 1, shardId, 2)
      )
    }

    "peek limit items from multiple shards" in {
      val stageName = "stage4"
      val shardId1 = 1
      val shardId2 = 2
      val batch = List(
        queueItem(stageName, "a", 1, shardId1, 1),
        queueItem(stageName, "b", 2, shardId1, 3),
        queueItem(stageName, "c", 3, shardId1, 5),
        queueItem(stageName, "d", 4, shardId2, 2),
        queueItem(stageName, "e", 5, shardId2, 4),
        queueItem(stageName, "f", 6, shardId2, 5)
      )
      doobieDatabase.masterTransaction(dao.offer(batch)(_)).futureValue
      val peekResult = doobieDatabase
        .masterTransaction(
          dao.peek(List(shardId1, shardId2), DefaultEntityType, stageName, 4, createTimestamp(10000))(_)
        )
        .futureValue
      peekResult shouldBe List(
        queueItem(stageName, "a", 1, shardId1, 1),
        queueItem(stageName, "d", 4, shardId2, 2),
        queueItem(stageName, "b", 2, shardId1, 3),
        queueItem(stageName, "e", 5, shardId2, 4)
      )
    }

    "remove fully matching items" in {
      val stageName = "stage5"
      val shardId = 1
      val batchToInsert = List(
        queueItem(stageName, "a", 1, shardId, 1),
        queueItem(stageName, "b", 2, shardId, 3),
        queueItem(stageName, "c", 3, shardId, 5),
        queueItem(stageName, "d", 4, shardId, 7)
      )
      val batchToRemove = List(
        queueItem(stageName, "a", 0, shardId, 1), // version is different
        queueItem(stageName, "b", 2, shardId, 2), // visitTime is different,
        queueItem(stageName, "c", 3, shardId, 5, entityType = "abacaba"), // entityType is different
        queueItem(stageName, "d", 4, shardId, 7) // everything matches
      )
      doobieDatabase.masterTransaction(dao.offer(batchToInsert)(_)).futureValue
      doobieDatabase.masterTransaction(dao.remove(batchToRemove)(_))
      val peekResult = doobieDatabase
        .masterTransaction(
          dao.peek(List(shardId), DefaultEntityType, stageName, 1000, createTimestamp(10000))(_)
        )
        .futureValue
      peekResult shouldBe List(
        queueItem(stageName, "a", 1, shardId, 1),
        queueItem(stageName, "b", 2, shardId, 3),
        queueItem(stageName, "c", 3, shardId, 5)
      )
    }
  }
}
