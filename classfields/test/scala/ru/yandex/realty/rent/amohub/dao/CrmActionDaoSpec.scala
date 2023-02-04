package ru.yandex.realty.rent.amohub.dao

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.amohub.proto.model.payload.ActionPayload
import ru.yandex.realty.rent.amohub.model.{CrmAction, CrmActionGroup, CrmActionStatus, CrmActionType}
import ru.yandex.realty.sharding.Shard
import ru.yandex.vertis.util.time.DateTimeUtil

import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class CrmActionDaoSpec extends AmohubDaoSpecBase {

  "CrmActionDao" should {
    "insert actions" in {
      val firstAction = action("123", "abc")
      val secondAction = action("456", "def")

      val insertedActions = crmActionDao.insertIfNotExist(Seq(firstAction, secondAction)).futureValue

      insertedActions.size shouldEqual 2
      insertedActions.foreach(_.dbId should not equal 0)
      insertedActions.exists(_.groupId == "123") shouldEqual true
      insertedActions.exists(_.groupId == "456") shouldEqual true
    }

    "insert actions without duplicates" in {
      val firstAction = action("123", "abc")
      val secondAction = action("456", "def")

      val firstlyInsertedActions = crmActionDao.insertIfNotExist(Seq(firstAction)).futureValue

      firstlyInsertedActions.size shouldEqual 1
      firstlyInsertedActions.head.groupId shouldEqual "123"

      val secondlyInsertedActions = crmActionDao.insertIfNotExist(Seq(firstAction, secondAction)).futureValue

      secondlyInsertedActions.size shouldEqual 1
      secondlyInsertedActions.head.groupId shouldEqual "456"

      val thirdlyInsertedActions = crmActionDao.insertIfNotExist(Seq(firstAction, secondAction)).futureValue

      thirdlyInsertedActions.size shouldEqual 0
    }

    "insert actions in predictable order" in {
      val actions = (1 to 10).map(id => action(id.toString, id.toString))

      val insertedActions = crmActionDao.insertIfNotExist(actions).futureValue

      (1 to 9).foreach { id =>
        val firstAction = insertedActions.find(_.groupId == id.toString).get
        val secondAction = insertedActions.find(_.groupId == (id + 1).toString).get
        (firstAction.dbId < secondAction.dbId) shouldEqual true
      }
    }

    "process single action by watch method" in {
      val singleAction = action("123", "abc")
      val insertedActions = crmActionDao.insertIfNotExist(Seq(singleAction)).futureValue

      val processedGroups = callWatch(expectedProcessCount = 1)

      processedGroups.size shouldEqual 1
      processedGroups.head.id shouldEqual "123"
      processedGroups.head.actions.size shouldEqual 1
      processedGroups.head.actions.head.dbId shouldEqual insertedActions.head.dbId
      assertActionsProcessed(Set(insertedActions.head.dbId))
    }

    "process two actions from different groups separately" in {
      val firstAction = action("123", "abc")
      val insertedFirstAction = crmActionDao.insertIfNotExist(Seq(firstAction)).futureValue.head
      val secondAction = action("456", "def")
      val insertedSecondAction = crmActionDao.insertIfNotExist(Seq(secondAction)).futureValue.head

      val processedGroups = callWatch(expectedProcessCount = 2)

      processedGroups.size shouldEqual 2
      processedGroups.map(_.id) should contain theSameElementsAs Iterable("123", "456")
      processedGroups.foreach(_.actions.size shouldEqual 1)
      assertActionsProcessed(Set(insertedFirstAction.dbId, insertedSecondAction.dbId))
    }

    "process two actions from the same group together" in {
      val firstAction = action("123", "abc")
      val insertedFirstAction = crmActionDao.insertIfNotExist(Seq(firstAction)).futureValue.head
      val secondAction = action("123", "def")
      val insertedSecondAction = crmActionDao.insertIfNotExist(Seq(secondAction)).futureValue.head
      val actionsDbIds = Set(insertedFirstAction.dbId, insertedSecondAction.dbId)

      val processedGroups = callWatch(expectedProcessCount = 1)

      processedGroups.size shouldEqual 1
      processedGroups.head.id shouldEqual "123"
      processedGroups.head.actions.size shouldEqual 2
      processedGroups.head.actions.map(_.dbId) should contain theSameElementsAs actionsDbIds
      assertActionsProcessed(actionsDbIds)
    }

    "process all new actions in group together whatever batch contains" in {
      val firstAction = action("123", "abc", 1.minute)
      val insertedFirstAction = crmActionDao.insertIfNotExist(Seq(firstAction)).futureValue.head
      val secondAction = action("456", "def", 45.seconds)
      val insertedSecondAction = crmActionDao.insertIfNotExist(Seq(secondAction)).futureValue.head
      val thirdAction = action("456", "fed", 30.seconds)
      val insertedThirdAction = crmActionDao.insertIfNotExist(Seq(thirdAction)).futureValue.head
      val actionsDbIds = Set(insertedFirstAction.dbId, insertedSecondAction.dbId, insertedThirdAction.dbId)

      // It's important, watchLimit is 2 here: we check that third action will be processed anyway
      val processedGroups = callWatch(expectedProcessCount = 2, watchLimit = 2)

      processedGroups.size shouldEqual 2
      processedGroups.map(_.id) should contain theSameElementsAs Iterable("123", "456")
      processedGroups.flatMap(_.actions).size shouldEqual 3
      processedGroups.flatMap(_.actions).map(_.dbId) should contain theSameElementsAs actionsDbIds
      assertActionsProcessed(actionsDbIds)
    }
  }

  private def callWatch(expectedProcessCount: Int, watchLimit: Int = 5): Iterable[CrmActionGroup] = {
    val processedGroups = new ConcurrentLinkedQueue[CrmActionGroup]
    val result = crmActionDao
      .watch(watchLimit, Shard(0, 1)) { group =>
        processedGroups.add(group)
        val updatedActions = group.actions.map { action =>
          action.copy(status = CrmActionStatus.Processed, retriesCount = 1, visitTime = None)
        }
        val updatedGroup = group.copy(actions = updatedActions)
        Future.successful(updatedGroup)
      }
      .futureValue

    result.processedCount shouldEqual expectedProcessCount
    result.failedCount shouldEqual 0
    processedGroups.asScala
  }

  private def assertActionsProcessed(actionDbIds: Set[Long]): Unit = {
    val processedActions = crmActionDao.getByIds(actionDbIds).futureValue
    processedActions.size shouldEqual actionDbIds.size
    processedActions.foreach { action =>
      action.status shouldEqual CrmActionStatus.Processed
      action.retriesCount shouldEqual 1
      action.visitTime shouldEqual None
    }
  }

  private def action(groupId: String, idempotencyKey: String, visitDelay: FiniteDuration = 30.seconds): CrmAction =
    CrmAction(
      actionType = CrmActionType.CreateShowing,
      idempotencyKey = idempotencyKey,
      groupId = groupId,
      status = CrmActionStatus.New,
      retriesCount = 0,
      createTime = DateTimeUtil.now().minusMinutes(1),
      lastAttemptTime = None,
      payload = ActionPayload.getDefaultInstance,
      visitTime = Some(DateTimeUtil.now().minus(visitDelay.toMillis)),
      shardKey = 0
    )
}
