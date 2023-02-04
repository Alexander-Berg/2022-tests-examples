package ru.yandex.complaints.watching.stages.offer

import java.sql.Timestamp

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.config.RankingInfoAware
import ru.yandex.complaints.dao._
import ru.yandex.complaints.dao.complaints.{CountingComplaintsDao, FailingACFOComplaintsDao}
import ru.yandex.complaints.model.User.UserId
import ru.yandex.complaints.model._
import ru.yandex.complaints.services.{CountingModerationService, FailingNotifyModerationService}
import ru.yandex.complaints.util.actors.Ranker.ComplaintsEnriched
import ru.yandex.complaints.util.actors.{DefaultRanker, Ranker}
import ru.yandex.complaints.watching.ProcessingState
import ru.yandex.complaints.watching.stages.RankingStage
import ru.yandex.complaints.watching.stages.RankingStage.EmptyComplaintPolicy

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
  * Created by s-reznick on 21.07.16.
  */
@RunWith(classOf[JUnitRunner])
class RankingStageSpec extends WordSpec with Matchers {

  import DefaultRanker._

  private implicit val ranker: Ranker = DefaultRanker

  val Now = new Timestamp(getNow)
  val Stage = "Stage"
  val OfferValue = Offer(
    id = Plain("o12345"),
    createTime = Now,
    authorId = "a123",
    hash = "somehash"
  )
  val Service = "service"

  trait DefaultThresholds {
    val thresholdCheck = 3.0f
    val thresholdBan = 5.1f
  }

  Stage should {
    "reschedule on failed attempt to call allComplaintsForOffer" in {
      object LocalComplaintsDao extends FailingACFOComplaintsDao
      object LocalModerationService extends CountingModerationService
      object LocalRankingInfoAware
        extends RankingInfoAware with DefaultThresholds {
        override val serviceName = Service
        override val complaintsDao = LocalComplaintsDao
      }

      object LocalStage
        extends RankingStage(LocalModerationService, EmptyComplaintPolicy)(LocalRankingInfoAware, implicitly)

      val state = new ProcessingState[Offer](OfferValue, 100 hours)

      val result = LocalStage.process(state)

      assert(result == state.copy(delay = 5 minutes))
      assert(LocalComplaintsDao.totalCount == 1)
      assert(LocalComplaintsDao.acfoCount == 1)
      assert(LocalModerationService.totalCount== 0)
    }
  }

  Stage should {
    "do nothing if no complaints are found" in {
      object LocalComplaintsDao extends CountingComplaintsDao {
        override def allComplaintsForOffer(offerId: OfferID)
        : Try[(Seq[Complaint], Map[UserId, User])] = Try {
          super.allComplaintsForOffer(offerId)

          (Seq[Complaint](), Map[UserId, User]())
        }
      }
      object LocalModerationService extends CountingModerationService
      object LocalRankingInfoAware
        extends RankingInfoAware with DefaultThresholds {
        override val serviceName = Service
        override val complaintsDao = LocalComplaintsDao
      }

      object LocalStage
        extends RankingStage(LocalModerationService, EmptyComplaintPolicy)(LocalRankingInfoAware, implicitly)

      val state = new ProcessingState[Offer](OfferValue, 100 hours)

      val result = LocalStage.process(state)

      assert(result eq state)
      assert(LocalComplaintsDao.totalCount == 1)
      assert(LocalComplaintsDao.acfoCount == 1)
      assert(LocalModerationService.totalCount== 0)
    }
  }

  val ComplaintSample = Complaint(
    userId = UserId("u12345"),
    userType = UserType.Undefined,
    offerId = Plain("o12345"),
    complaintId = "c12345",
    modobjId = None,
    ctype = ComplaintType.Commercial,
    description = "Some text",
    created = new Timestamp(getNow),
    userData = Complaint.UserData.Empty,
    notified = false,
    source = None
  )

  val BadUser = User(
    UserId("gu12345"),
    UserType.Undefined,
    -0.5f,
    0.5f,
    fixedRating = false,
    isVip = false,
    isAuto = false)

  val GoodUser = User(
    UserId("bu12345"),
    UserType.Undefined,
    0.5f,
    -0.5f,
    fixedRating = false,
    isVip = false,
    isAuto = false)


  Stage should {
    "do nothing if offer rank is below threshold" in {
      val forOffer = (Seq(ComplaintSample.copy(userId = BadUser.id)),
        Map(BadUser.id -> BadUser))

      object LocalComplaintsDao extends CountingComplaintsDao {

        override def allComplaintsForOffer(offerId: OfferID)
        : Try[(Seq[Complaint], Map[UserId, User])] = Try {
          super.allComplaintsForOffer(offerId)

          forOffer
        }
      }

      object LocalRankingInfoAware
        extends RankingInfoAware with DefaultThresholds {
        override val serviceName = Service
        override val complaintsDao = LocalComplaintsDao
      }

      object LocalModerationService extends CountingModerationService

      object LocalStage
        extends RankingStage(LocalModerationService, EmptyComplaintPolicy)(LocalRankingInfoAware, implicitly)

      assert(
        offerRank(ComplaintsEnriched(forOffer)).get <=
          LocalRankingInfoAware.thresholdCheck,
        "precondition")

      val state = new ProcessingState[Offer](OfferValue, 100 hours)

      val result = LocalStage.process(state)

      assert(result eq state)
      assert(LocalComplaintsDao.totalCount == 1)
      assert(LocalComplaintsDao.acfoCount == 1)
      assert(LocalModerationService.totalCount == 0)
    }
  }

  class HighRankComplaintsDao(threshold: Float) extends CountingComplaintsDao {
    val forOffer = (Seq(ComplaintSample.copy(userId = GoodUser.id)),
      Map(GoodUser.id -> GoodUser))

    override def allComplaintsForOffer(offerId: OfferID)
    : Try[(Seq[Complaint], Map[UserId, User])] = Try {
      super.allComplaintsForOffer(offerId)

      forOffer
    }

    assert(
      offerRank(
        ComplaintsEnriched(forOffer)).get > threshold, "precondition")
  }

  Stage should {
    "notify moderation service if offer rank is above threasold" +
      " but do nothing next time with the same offer" in {
      object LocalModerationService extends CountingModerationService
      //object LocalComplaintsDao extends HighRankComplaintsDao
      object LocalRankingInfoAware
        extends RankingInfoAware with DefaultThresholds {
        override val serviceName = Service
        override val complaintsDao = new HighRankComplaintsDao(thresholdCheck)
      }

      object LocalStage
        extends RankingStage(LocalModerationService, EmptyComplaintPolicy)(LocalRankingInfoAware, implicitly)

      val state = new ProcessingState[Offer](OfferValue, 100 hours)

      val result = LocalStage.process(state)

      assert(result.delay == state.delay)
      assert(result.elem.hash != state.elem.hash)
      assert(result.elem.copy(hash = "") == state.elem.copy(hash = ""))
      assert(LocalRankingInfoAware.complaintsDao.totalCount == 1)
      assert(LocalRankingInfoAware.complaintsDao.acfoCount == 1)
      assert(LocalModerationService.totalCount == 1)
      assert(LocalModerationService.notifyCount == 1)

      val state2 = new ProcessingState[Offer](result.elem, 100 hours)
      LocalStage.process(state2)
    }
  }

  Stage should {
    "reschedule on failed attempt to notify moderation service" in {
      object LocalModerationService extends FailingNotifyModerationService
      object LocalRankingInfoAware
        extends RankingInfoAware with DefaultThresholds {
        override val serviceName = Service
        override val complaintsDao = new HighRankComplaintsDao(thresholdCheck)
      }

      object LocalStage
        extends RankingStage(LocalModerationService, EmptyComplaintPolicy)(LocalRankingInfoAware, implicitly)

      val state = new ProcessingState[Offer](OfferValue, 100 hours)

      val result = LocalStage.process(state)

      assert(result == state.copy(delay = 5 minutes))
      assert(LocalRankingInfoAware.complaintsDao.totalCount == 1)
      assert(LocalRankingInfoAware.complaintsDao.acfoCount == 1)
      assert(LocalModerationService.totalCount == 1)
      assert(LocalModerationService.notifyCount == 1)
    }
  }
}