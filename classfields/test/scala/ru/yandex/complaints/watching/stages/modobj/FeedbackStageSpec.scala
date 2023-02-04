package ru.yandex.complaints.watching.stages.modobj

import java.sql.Timestamp

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.dao._
import ru.yandex.complaints.dao.complaints.CountingComplaintsDao
import ru.yandex.complaints.dao.complaints.FailingFeedbackComplaintsDao
import ru.yandex.complaints.model.{Decision, ModObj}
import ru.yandex.complaints.watching.ProcessingState
import ru.yandex.complaints.watching.stages.FeedbackStage

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by s-reznick on 21.07.16.
  */
@RunWith(classOf[JUnitRunner])
class FeedbackStageSpec extends WordSpec with Matchers {

  val Seconds1 = 1000
  val Seconds60 = 60000
  val Now = new Timestamp(getNow)
  val SecondAgo = new Timestamp(Now.getTime - Seconds1)
  val MinuteAgo = new Timestamp(Now.getTime - Seconds60)
  val Stage = "Stage"
  val OfferId = Plain("o12345")
  val ModObjId = "m12345"

  Stage should {
    "do nothing if response time is not defined" in {
      object LocalComplaintsDao extends CountingComplaintsDao

      object LocalStage extends FeedbackStage(LocalComplaintsDao)

      val modobj = ModObj(
        offerId = OfferId,
        modobjId = ModObjId,
        decision = Option(Decision.Accepted),
        askTime = Option(MinuteAgo),
        responseTime = None,
        feedbackTime = Option(SecondAgo)
      )

      val state = new ProcessingState[ModObj](modobj, 100 hours)

      val result = LocalStage.process(state)

      assert(result eq state)
      assert(LocalComplaintsDao.totalCount == 0)
    }
  }

  Stage should {
    "do nothing if feedback time is present" +
      " and is after the response time" in {
      object LocalComplaintsDao extends CountingComplaintsDao

      object LocalStage extends FeedbackStage(LocalComplaintsDao)

      val modobj = new ModObj(
        offerId = OfferId,
        modobjId = ModObjId,
        decision = Option(Decision.Accepted),
        askTime = Option(MinuteAgo),
        responseTime = Option(SecondAgo),
        feedbackTime = Option(Now)
      )

      val state = new ProcessingState[ModObj](modobj, 100 hours)

      val result = LocalStage.process(state)

      assert(result eq state)
      assert(LocalComplaintsDao.totalCount == 0)
    }
  }

  Stage should {
    "try to provide feedback if feedback time is absent" +
      " and reschedule on exception inside feedback method" in {
      object LocalComplaintsDao extends FailingFeedbackComplaintsDao

      object LocalStage extends FeedbackStage(LocalComplaintsDao)

      val modobj = new ModObj(
        offerId = OfferId,
        modobjId = ModObjId,
        decision = Some(Decision.Accepted),
        askTime = Option(MinuteAgo),
        responseTime = Option(Now),
        feedbackTime = None
      )

      val state = new ProcessingState[ModObj](modobj, 100 hours)

      val result = LocalStage.process(state)

      assert(result == state.copy(delay = 5 minutes))
      assert(LocalComplaintsDao.totalCount == 1)
      assert(LocalComplaintsDao.feedbackCount == 1)
      assert(LocalComplaintsDao.feedbackNegCount == 1)
    }
  }

  Stage should {
    "try to provide feedback if feedback time is" +
      " before response time and reschedule on exception" +
      " inside feedback method" in {
      object LocalComplaintsDao extends FailingFeedbackComplaintsDao

      object LocalStage extends FeedbackStage(LocalComplaintsDao)

      val modobj = new ModObj(
        offerId = OfferId,
        modobjId = ModObjId,
        decision = Some(Decision.Banned),
        askTime = Option(MinuteAgo),
        responseTime = Option(Now),
        feedbackTime = Option(SecondAgo)
      )

      val state = new ProcessingState[ModObj](modobj, 100 hours)

      val result = LocalStage.process(state)

      assert(result == state.copy(delay = 5 minutes))
      assert(LocalComplaintsDao.totalCount == 1)
      assert(LocalComplaintsDao.feedbackCount == 1)
      assert(LocalComplaintsDao.feedbackPosCount == 1)
    }
  }

  Stage should {
    "provide feedback if feedback time is before response time" in {
      object LocalComplaintsDao extends CountingComplaintsDao

      object LocalStage extends FeedbackStage(LocalComplaintsDao)

      val modobj = new ModObj(
        offerId = OfferId,
        modobjId = ModObjId,
        decision = Some(Decision.Banned),
        askTime = Option(MinuteAgo),
        responseTime = Option(Now),
        feedbackTime = Option(SecondAgo)
      )

      val state = new ProcessingState[ModObj](modobj, 100 hours)

      val result = LocalStage.process(state)

      assert(result eq state)
      assert(LocalComplaintsDao.totalCount == 1)
      assert(LocalComplaintsDao.feedbackCount == 1)
      assert(LocalComplaintsDao.feedbackPosCount == 1)
    }
  }

  Stage should {
    "provide feedback if feedback time is None" in {
      object LocalComplaintsDao extends CountingComplaintsDao

      object LocalStage extends FeedbackStage(LocalComplaintsDao)

      val modobj = new ModObj(
        offerId = OfferId,
        modobjId = ModObjId,
        decision = Some(Decision.Accepted),
        askTime = Option(MinuteAgo),
        responseTime = Option(Now),
        feedbackTime = None
      )

      val state = new ProcessingState[ModObj](modobj, 100 hours)

      val result = LocalStage.process(state)

      assert(result eq state)
      assert(LocalComplaintsDao.totalCount == 1)
      assert(LocalComplaintsDao.feedbackCount == 1)
      assert(LocalComplaintsDao.feedbackNegCount == 1)
    }
  }
}