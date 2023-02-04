package ru.yandex.realty.rent.amohub.scheduler.stage

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.amohub.clients.amocrm.{AccessTokenProvider, AmocrmClient}
import ru.yandex.realty.amohub.proto.model.payload.{ActionPayload, CreateShowingPayload}
import ru.yandex.realty.rent.amohub.model.CrmActionType.CrmActionType
import ru.yandex.realty.rent.amohub.model.{CrmAction, CrmActionGroup, CrmActionStatus, CrmActionType}
import ru.yandex.realty.rent.amohub.scheduler.processor.{
  CrmActionProcessor,
  CrmActionResult,
  FailedWithUpdate,
  Processed,
  ProcessedWithUpdate
}
import ru.yandex.realty.rent.amohub.scheduler.stage.crm.action.ProcessActionStage
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.gen.IdGenerator
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ProcessActionStageSpec extends SpecBase with AsyncSpecBase {

  "ProcessActionStage" should {
    "not process if last attempt was recently" in {
      val now = DateTimeUtil.now()
      val singleAction = action(dbId = 1, retriesCount = 2, lastAttemptTime = Some(now))
      val group = CrmActionGroup(Seq(singleAction))

      val processedGroup = processGroup(group, successProcessor)

      val processedAction = processedGroup.actions.head
      processedAction.retriesCount shouldEqual 2
      processedAction.status shouldEqual CrmActionStatus.New
      processedAction.lastAttemptTime shouldEqual Some(now)
      processedAction.visitTime.exists(_.isAfter(now)) shouldEqual true
      processedAction.payload shouldEqual singleAction.payload
    }

    "successfully process single action" in {
      val singleAction = action(dbId = 1)
      val group = CrmActionGroup(Seq(singleAction))

      val processedGroup = processGroup(group, successProcessor)

      val processedAction = processedGroup.actions.head
      processedAction.retriesCount shouldEqual 1
      processedAction.status shouldEqual CrmActionStatus.Processed
      processedAction.lastAttemptTime.nonEmpty shouldEqual true
      processedAction.visitTime shouldEqual None
      processedAction.payload shouldEqual singleAction.payload
    }

    "successfully process single action with payload update" in {
      val singleAction = action(dbId = 1)
      val group = CrmActionGroup(Seq(singleAction))

      val processedGroup = processGroup(group, successWithUpdateProcessor)

      val processedAction = processedGroup.actions.head
      processedAction.retriesCount shouldEqual 1
      processedAction.status shouldEqual CrmActionStatus.Processed
      processedAction.lastAttemptTime.nonEmpty shouldEqual true
      processedAction.visitTime shouldEqual None
      processedAction.payload shouldEqual processedPayload()
    }

    "process earliest action and reschedule another one" in {
      val firstAction = action(dbId = 1)
      val secondAction = action(dbId = 2)
      val group = CrmActionGroup(Seq(secondAction, firstAction))

      val processedGroup = processGroup(group, successProcessor)

      val processedFirstAction = processedGroup.actions.find(_.dbId == 1).get
      processedFirstAction.retriesCount shouldEqual 1
      processedFirstAction.status shouldEqual CrmActionStatus.Processed
      processedFirstAction.lastAttemptTime.nonEmpty shouldEqual true
      processedFirstAction.visitTime shouldEqual None
      processedFirstAction.payload shouldEqual firstAction.payload

      val processedSecondAction = processedGroup.actions.find(_.dbId == 2).get
      processedSecondAction.retriesCount shouldEqual 0
      processedSecondAction.status shouldEqual CrmActionStatus.New
      processedSecondAction.lastAttemptTime shouldEqual None
      processedSecondAction.visitTime.nonEmpty shouldEqual true
      processedSecondAction.payload shouldEqual secondAction.payload
    }

    "revisit failed action" in {
      val now = DateTimeUtil.now()
      val lastAttemptTime = now.minusMinutes(2)
      val firstAction = action(dbId = 1, retriesCount = 4, lastAttemptTime = Some(lastAttemptTime))
      val secondAction = action(dbId = 2)
      val group = CrmActionGroup(Seq(secondAction, firstAction))

      val processedGroup = processGroup(group, failureProcessor)

      val processedFirstAction = processedGroup.actions.find(_.dbId == 1).get
      processedFirstAction.retriesCount shouldEqual 5
      processedFirstAction.status shouldEqual CrmActionStatus.New
      processedFirstAction.lastAttemptTime.exists(_.isAfter(lastAttemptTime)) shouldEqual true
      processedFirstAction.visitTime.exists(_.isAfter(now)) shouldEqual true
      processedFirstAction.payload shouldEqual firstAction.payload

      val processedSecondAction = processedGroup.actions.find(_.dbId == 2).get
      processedSecondAction.retriesCount shouldEqual 0
      processedSecondAction.status shouldEqual CrmActionStatus.New
      processedSecondAction.lastAttemptTime shouldEqual None
      processedSecondAction.visitTime.exists(_.isAfter(now)) shouldEqual true
      processedSecondAction.payload shouldEqual secondAction.payload
    }

    "mark failed action with Error status" in {
      val now = DateTimeUtil.now()
      val lastAttemptTime = now.minusMinutes(2)
      val singleAction = action(dbId = 1, retriesCount = 9, lastAttemptTime = Some(lastAttemptTime))
      val group = CrmActionGroup(Seq(singleAction))

      val processedGroup = processGroup(group, failureProcessor)

      val processedAction = processedGroup.actions.head
      processedAction.retriesCount shouldEqual 10
      processedAction.status shouldEqual CrmActionStatus.Error
      processedAction.lastAttemptTime.exists(_.isAfter(lastAttemptTime)) shouldEqual true
      processedAction.visitTime shouldEqual None
      processedAction.payload shouldEqual singleAction.payload
    }

    "revisit failed action with payload update" in {
      val now = DateTimeUtil.now()
      val lastAttemptTime = now.minusMinutes(2)
      val singleAction = action(dbId = 1, retriesCount = 4, lastAttemptTime = Some(lastAttemptTime))
      val group = CrmActionGroup(Seq(singleAction))

      val processedGroup = processGroup(group, failureWithUpdateProcessor)

      val processedAction = processedGroup.actions.head
      processedAction.retriesCount shouldEqual 5
      processedAction.status shouldEqual CrmActionStatus.New
      processedAction.lastAttemptTime.exists(_.isAfter(lastAttemptTime)) shouldEqual true
      processedAction.visitTime.exists(_.isAfter(now)) shouldEqual true
      processedAction.payload shouldEqual processedPayload()
    }
  }

  private def processGroup(group: CrmActionGroup, processor: CrmActionProcessor): CrmActionGroup = {
    val state = ProcessingState(group)
    val accessTokenProvider = AccessTokenProvider.fromToken(AmocrmClient.AccessToken("token"))
    val stage = new ProcessActionStage(Seq(processor), accessTokenProvider)
    val updatedState = stage.process(state)(Traced.empty).futureValue
    updatedState.entry
  }

  private def successProcessor: CrmActionProcessor =
    buildProcessor(Future.successful(Processed))

  private def successWithUpdateProcessor: CrmActionProcessor =
    buildProcessor(Future.successful(ProcessedWithUpdate(processedPayload())))

  private def failureProcessor: CrmActionProcessor =
    buildProcessor(Future.failed(new RuntimeException))

  private def failureWithUpdateProcessor: CrmActionProcessor =
    buildProcessor(Future.successful(FailedWithUpdate(processedPayload(), new RuntimeException)))

  private def buildProcessor(resultFuture: Future[CrmActionResult]): CrmActionProcessor = new CrmActionProcessor {
    override def actionType: CrmActionType = CrmActionType.CreateShowing
    override def process(
      action: CrmAction
    )(implicit traced: Traced, accessToken: AmocrmClient.AccessToken): Future[CrmActionResult] = resultFuture
  }

  private def action(dbId: Long, retriesCount: Int = 0, lastAttemptTime: Option[DateTime] = None): CrmAction =
    CrmAction(
      dbId = dbId,
      actionType = CrmActionType.CreateShowing,
      idempotencyKey = IdGenerator.generateUuid(),
      groupId = "123",
      status = CrmActionStatus.New,
      retriesCount = retriesCount,
      createTime = DateTimeUtil.now().minusMinutes(1),
      lastAttemptTime = lastAttemptTime,
      payload = emptyPayload(),
      visitTime = None,
      shardKey = 0
    )

  private def emptyPayload(): ActionPayload =
    buildPayload(0)

  private def processedPayload(): ActionPayload =
    buildPayload(1)

  private def buildPayload(createdLeadId: Long): ActionPayload =
    ActionPayload
      .newBuilder()
      .setCreateShowing {
        CreateShowingPayload
          .newBuilder()
          .setCreatedLeadId(createdLeadId)
      }
      .build()
}
