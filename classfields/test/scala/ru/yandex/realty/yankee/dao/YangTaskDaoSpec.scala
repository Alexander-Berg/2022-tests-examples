package ru.yandex.realty.yankee.dao

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.watching2.WatchableDao.{Invalidated, UpdateSuccessful}
import ru.yandex.realty.yankee.model.{YangTask, YangTaskDeliveryStatus, YangTaskStatus, YangTaskType}
import ru.yandex.realty.yankee.proto.model.payload.payload.YangTaskPayload
import ru.yandex.realty.yankee.proto.model.result.result.YangTaskResult

import java.time.Instant

@RunWith(classOf[JUnitRunner])
class YangTaskDaoSpec extends BaseYankeeDaoSpec with Matchers {

  private val FirstIdempotencyKey = "123"
  private val SecondIdempotencyKey = "456"
  private val YangOperationId = "789"

  "YangTaskDao" should {
    "insert tasks" in {
      val firstTask = buildTask(FirstIdempotencyKey)
      val secondTask = buildTask(SecondIdempotencyKey)

      val insertedTasks = doobieDatabase.masterTransaction {
        yangTaskDao.insert(Iterable(firstTask, secondTask))(_)
      }.futureValue

      insertedTasks.size shouldEqual 2
      insertedTasks.map(_.idempotencyKey) shouldEqual Iterable(FirstIdempotencyKey, SecondIdempotencyKey)
      insertedTasks.foreach(_.id shouldNot equal(0))
    }

    "insert and get tasks" in {
      val firstTask = buildTask(FirstIdempotencyKey)
      val secondTask = buildTask(SecondIdempotencyKey)

      val insertedTasks = doobieDatabase.masterTransaction {
        yangTaskDao.insert(Iterable(firstTask, secondTask))(_)
      }.futureValue

      val firstTaskId = insertedTasks.find(_.idempotencyKey == FirstIdempotencyKey).get.id
      val secondTaskId = insertedTasks.find(_.idempotencyKey == SecondIdempotencyKey).get.id

      val firstSearchResult = doobieDatabase.replicaTransaction {
        yangTaskDao.get(Iterable(firstTaskId, -123))(_)
      }.futureValue

      firstSearchResult.size shouldEqual 1
      firstSearchResult.head.id shouldEqual firstTaskId
      firstSearchResult.head.idempotencyKey shouldEqual FirstIdempotencyKey

      val secondSearchResult = doobieDatabase.replicaTransaction {
        yangTaskDao.get(Iterable(firstTaskId, secondTaskId))(_)
      }.futureValue

      secondSearchResult.size shouldEqual 2
      secondSearchResult.map(_.id) shouldEqual Iterable(firstTaskId, secondTaskId)
      secondSearchResult.map(_.idempotencyKey) shouldEqual Iterable(FirstIdempotencyKey, SecondIdempotencyKey)

      val thirdSearchResult = doobieDatabase.replicaTransaction {
        yangTaskDao.get(Iterable.empty)(_)
      }.futureValue

      thirdSearchResult.size shouldEqual 0
    }

    "ignore duplicates inserting tasks" in {
      val firstTask = buildTask(FirstIdempotencyKey)
      val secondTask = buildTask(SecondIdempotencyKey)

      val firstlyInsertedTasks = doobieDatabase.masterTransaction {
        yangTaskDao.insert(Iterable(firstTask))(_)
      }.futureValue

      firstlyInsertedTasks.size shouldEqual 1
      firstlyInsertedTasks.head.idempotencyKey shouldEqual FirstIdempotencyKey

      val secondlyInsertedTasks = doobieDatabase.masterTransaction {
        yangTaskDao.insert(Iterable(firstTask, secondTask))(_)
      }.futureValue

      secondlyInsertedTasks.size shouldEqual 1
      secondlyInsertedTasks.head.idempotencyKey shouldEqual SecondIdempotencyKey
    }

    "update single entity" in {
      val task = buildTask(FirstIdempotencyKey)

      val insertedTasks = doobieDatabase.masterTransaction {
        yangTaskDao.insert(Iterable(task))(_)
      }.futureValue

      val updatedTask = insertedTasks.head.copy(
        status = YangTaskStatus.Initiated,
        yangOperationId = Some(YangOperationId),
        yangOperationCreateTime = Some(Instant.now()),
        result = Some(YangTaskResult()),
        deliveryStatus = YangTaskDeliveryStatus.Delivered
      )

      val updateResultMap = doobieDatabase.masterTransaction {
        yangTaskDao.updateWithVersionCheck(Iterable(updatedTask))(_)
      }.futureValue

      updateResultMap.size shouldEqual 1
      updateResultMap.head._1 shouldEqual insertedTasks.head.id
      updateResultMap.head._2 shouldEqual UpdateSuccessful

      val resultTasks = doobieDatabase.replicaTransaction {
        yangTaskDao.get(Iterable(insertedTasks.head.id))(_)
      }.futureValue

      resultTasks.size shouldEqual 1
      resultTasks.head.status shouldEqual updatedTask.status
      resultTasks.head.yangOperationId shouldEqual updatedTask.yangOperationId
      resultTasks.head.yangOperationCreateTime shouldEqual updatedTask.yangOperationCreateTime
      resultTasks.head.result shouldEqual updatedTask.result
      resultTasks.head.deliveryStatus shouldEqual updatedTask.deliveryStatus
      resultTasks.head.version shouldEqual task.version + 1
    }

    "update entities with different versions" in {
      val firstTask = buildTask(FirstIdempotencyKey)
      val secondTask = buildTask(SecondIdempotencyKey)

      val insertedTasks = doobieDatabase.masterTransaction {
        yangTaskDao.insert(Iterable(firstTask, secondTask))(_)
      }.futureValue

      val firstTaskId = insertedTasks.find(_.idempotencyKey == FirstIdempotencyKey).get.id
      val secondTaskId = insertedTasks.find(_.idempotencyKey == SecondIdempotencyKey).get.id

      val updatedTasks = insertedTasks.map(_.copy(status = YangTaskStatus.Initiated))
      val firstUpdatedTask = updatedTasks.find(_.idempotencyKey == FirstIdempotencyKey).get

      val firstUpdateResultMap = doobieDatabase.masterTransaction {
        yangTaskDao.updateWithVersionCheck(Iterable(firstUpdatedTask))(_)
      }.futureValue

      firstUpdateResultMap.size shouldEqual 1
      firstUpdateResultMap(firstTaskId) shouldEqual UpdateSuccessful

      val secondUpdateResultMap = doobieDatabase.masterTransaction {
        yangTaskDao.updateWithVersionCheck(updatedTasks)(_)
      }.futureValue

      secondUpdateResultMap.size shouldEqual 2
      secondUpdateResultMap(firstTaskId) shouldEqual Invalidated
      secondUpdateResultMap(secondTaskId) shouldEqual UpdateSuccessful
    }
  }

  private def buildTask(idempotencyKey: String): YangTask =
    YangTask(
      taskType = YangTaskType.PassportMainPageMarkup,
      payload = YangTaskPayload(),
      idempotencyKey = idempotencyKey,
      status = YangTaskStatus.New,
      yangOperationId = None,
      yangOperationCreateTime = None,
      result = None,
      deliveryStatus = YangTaskDeliveryStatus.NotDelivered,
      createTime = Instant.now(),
      updateTime = Instant.now()
    )
}
