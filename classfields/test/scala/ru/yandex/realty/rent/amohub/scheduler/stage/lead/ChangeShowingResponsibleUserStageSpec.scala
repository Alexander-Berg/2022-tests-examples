package ru.yandex.realty.rent.amohub.scheduler.stage.lead

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.amohub.clients.amocrm.PipelineConfig
import ru.yandex.realty.rent.amohub.application.AmocrmPipelineConfig
import ru.yandex.realty.rent.amohub.backend.converters.LeadStatus
import ru.yandex.realty.rent.amohub.backend.manager.responsible.ResponsibleUserManager
import ru.yandex.realty.rent.amohub.dao.CrmActionDao
import ru.yandex.realty.rent.amohub.gen.AmohubModelsGen
import ru.yandex.realty.rent.amohub.model.{CrmAction, CrmActionType, RentLead => Lead}
import ru.yandex.realty.rent.amohub.util.ChangeShowingResponsibleUserActionUtil
import ru.yandex.realty.rent.proto.api.showing.FlatShowingStatusNamespace.FlatShowingStatus
import ru.yandex.realty.rent.proto.api.showing.FlatShowingTypeNamespace.FlatShowingType
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils
import ru.yandex.realty.watching.ProcessingState

import java.time.LocalDate
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ChangeShowingResponsibleUserStageSpec extends AsyncSpecBase with AmohubModelsGen {

  implicit val traced: Traced = Traced.empty

  private val crmActionDao = mock[CrmActionDao]
  private val responsibleUserManager = mock[ResponsibleUserManager]
  private val pipelinesConfig: AmocrmPipelineConfig = AmocrmPipelineConfig(
    requests = PipelineConfig(pipelineId = 1, responsibleUserId = None),
    offers = PipelineConfig(pipelineId = 2, responsibleUserId = None),
    showings = PipelineConfig(pipelineId = 3, responsibleUserId = None),
    moveOuts = PipelineConfig(pipelineId = 4, responsibleUserId = None)
  )

  private def invokeStage(lead: Lead, clock: java.time.Clock): ProcessingState[Lead] = {
    val state = ProcessingState(lead)
    val stage = new ChangeShowingResponsibleUserStage(crmActionDao, responsibleUserManager, pipelinesConfig, clock)
    stage.process(state).futureValue
  }

  private def generateShowing(): Lead =
    leadGen.next.copy(
      statusId = LeadStatus.Testing.Showings.NewShowing,
      pipelineId = pipelinesConfig.showings.pipelineId,
      managerId = Some(posNum[Int].next)
    )

  private def responsibleIsCorrect(isCorrect: Boolean) =
    (responsibleUserManager
      .checkShowingResponsibleUserIsCorrect(_: FlatShowingStatus, _: FlatShowingType, _: Long))
      .expects(*, *, *)
      .returning(isCorrect)

  private def actionsWereNotInserted() =
    (crmActionDao.insertIfNotExist(_: Seq[CrmAction])(_: Traced)).expects(*, *).never()

  private def actionWasInserted(lead: Lead) =
    (crmActionDao
      .insertIfNotExist(_: Seq[CrmAction])(_: Traced))
      .expects(where { (crmActions: Seq[CrmAction], _) =>
        crmActions.size == 1 &&
        crmActions.head.actionType == CrmActionType.ChangeShowingResponsibleUser &&
        crmActions.head.payload.getChangeShowingResponsibleUser.getLeadId == lead.leadId
      })

  private def invokeStageAndCheckNewVisitTime(
    showing: Lead,
    invocationTimeClock: java.time.Clock,
    newVisitTimeIntervalStart: java.time.Instant,
    newVisitTimeIntervalEnd: java.time.Instant
  ) = {
    val newState = invokeStage(showing, invocationTimeClock)
    newState.entry.visitTime shouldNot be(empty)
    val visitTime = newState.entry.visitTime.get
    visitTime.plusSeconds(1).isAfter(newVisitTimeIntervalStart.toEpochMilli) &&
    visitTime.minusSeconds(1).isBefore(newVisitTimeIntervalEnd.toEpochMilli) shouldBe true
  }

  private val testLocalDate = LocalDate.of(2022, 3, 17)

  private val todayFirstRegularUpdateIntervalStart = testLocalDate
    .atTime(ChangeShowingResponsibleUserStage.PeriodicUpdateBaseTimePoints.head)
    .atZone(TimeUtils.MSK)
    .toInstant
  private val todayFirstRegularUpdateIntervalEnd = testLocalDate
    .atTime(ChangeShowingResponsibleUserStage.PeriodicUpdateBaseTimePoints.head)
    .plus(ChangeShowingResponsibleUserStage.PeriodicUpdatesInterval)
    .atZone(TimeUtils.MSK)
    .toInstant

  private val todaySecondRegularUpdateIntervalStart = testLocalDate
    .atTime(ChangeShowingResponsibleUserStage.PeriodicUpdateBaseTimePoints(1))
    .atZone(TimeUtils.MSK)
    .toInstant
  private val todaySecondRegularUpdateIntervalEnd = testLocalDate
    .atTime(ChangeShowingResponsibleUserStage.PeriodicUpdateBaseTimePoints(1))
    .plus(ChangeShowingResponsibleUserStage.PeriodicUpdatesInterval)
    .atZone(TimeUtils.MSK)
    .toInstant

  private val tomorrowFirstRegularUpdateIntervalStart = testLocalDate
    .plusDays(1)
    .atTime(ChangeShowingResponsibleUserStage.PeriodicUpdateBaseTimePoints.head)
    .atZone(TimeUtils.MSK)
    .toInstant
  private val tomorrowFirstRegularUpdateIntervalEnd = testLocalDate
    .plusDays(1)
    .atTime(ChangeShowingResponsibleUserStage.PeriodicUpdateBaseTimePoints.head)
    .plus(ChangeShowingResponsibleUserStage.PeriodicUpdatesInterval)
    .atZone(TimeUtils.MSK)
    .toInstant

  classOf[ChangeShowingResponsibleUserStage].getSimpleName should {
    "not create crmActions at night" in {
      val showing = generateShowing()

      responsibleIsCorrect(false).noMoreThanOnce()
      actionsWereNotInserted()

      val nightStart = testLocalDate.atTime(0, 0).atZone(TimeUtils.MSK).toInstant
      val nightStartClock = java.time.Clock.fixed(nightStart, TimeUtils.MSK)
      invokeStageAndCheckNewVisitTime(
        showing,
        nightStartClock,
        todayFirstRegularUpdateIntervalStart,
        todayFirstRegularUpdateIntervalEnd
      )

      val nightEnd = testLocalDate.atTime(0, 0).atZone(TimeUtils.MSK).toInstant
      val nightEndClock = java.time.Clock.fixed(nightEnd, TimeUtils.MSK)
      invokeStageAndCheckNewVisitTime(
        showing,
        nightEndClock,
        todayFirstRegularUpdateIntervalStart,
        todayFirstRegularUpdateIntervalEnd
      )
    }

    "not create crmActions if responsible is correct" in {
      val showing = generateShowing()

      responsibleIsCorrect(true)
      actionsWereNotInserted()

      val invocationTime = testLocalDate.atTime(18, 0).atZone(TimeUtils.MSK).toInstant
      val invocationTimeClock = java.time.Clock.fixed(invocationTime, TimeUtils.MSK)

      invokeStageAndCheckNewVisitTime(
        showing,
        invocationTimeClock,
        tomorrowFirstRegularUpdateIntervalStart,
        tomorrowFirstRegularUpdateIntervalEnd
      )
    }

    "reschedule lead to closest update interval" in {
      val showing = generateShowing()

      val invocationTime = todayFirstRegularUpdateIntervalEnd.plusSeconds(100)
      val invocationTimeClock = java.time.Clock.fixed(invocationTime, TimeUtils.MSK)

      responsibleIsCorrect(false)
      actionWasInserted(showing).onCall((actions: Seq[CrmAction], _) => Future.successful(actions)).once()

      invokeStageAndCheckNewVisitTime(
        showing,
        invocationTimeClock,
        todaySecondRegularUpdateIntervalStart,
        todaySecondRegularUpdateIntervalEnd
      )
    }

    "create crmAction if responsible is not correct and time is not night" in {
      val showing = generateShowing()

      val invocationTime = testLocalDate.atTime(18, 0).atZone(TimeUtils.MSK).toInstant
      val invocationTimeClock = java.time.Clock.fixed(invocationTime, TimeUtils.MSK)

      responsibleIsCorrect(false)
      actionWasInserted(showing).onCall((actions: Seq[CrmAction], _) => Future.successful(actions)).once()

      invokeStageAndCheckNewVisitTime(
        showing,
        invocationTimeClock,
        tomorrowFirstRegularUpdateIntervalStart,
        tomorrowFirstRegularUpdateIntervalEnd
      )
    }

    "create crmAction if showing has no responsible" in {
      val showing = generateShowing().copy(managerId = None)

      val invocationTime = testLocalDate.atTime(18, 0).atZone(TimeUtils.MSK).toInstant
      val invocationTimeClock = java.time.Clock.fixed(invocationTime, TimeUtils.MSK)

      responsibleIsCorrect(true).never()
      actionWasInserted(showing).onCall((actions: Seq[CrmAction], _) => Future.successful(actions)).once()

      invokeStageAndCheckNewVisitTime(
        showing,
        invocationTimeClock,
        tomorrowFirstRegularUpdateIntervalStart,
        tomorrowFirstRegularUpdateIntervalEnd
      )
    }

    "reschedule stage in 10 minutes if crmAction cannot be created" in {
      val showing = generateShowing()

      val invocationTime = testLocalDate.atTime(18, 0).atZone(TimeUtils.MSK).toInstant
      val invocationTimeClock = java.time.Clock.fixed(invocationTime, TimeUtils.MSK)

      responsibleIsCorrect(false)
      actionWasInserted(showing).onCall((_: Seq[CrmAction], _) => Future.successful(Vector.empty)).once()

      invokeStageAndCheckNewVisitTime(
        showing,
        invocationTimeClock,
        invocationTime.plus(ChangeShowingResponsibleUserActionUtil.ActionDeduplicationPeriod),
        invocationTime.plus(ChangeShowingResponsibleUserActionUtil.ActionDeduplicationPeriod)
      )
    }
  }
}
