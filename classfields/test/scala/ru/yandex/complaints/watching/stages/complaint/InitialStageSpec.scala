package ru.yandex.complaints.watching.stages.complaint

import java.sql.Timestamp

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.dao._
import ru.yandex.complaints.dao.complaints.{CountingComplaintsDao, FailingSMOIComplaintsDao}
import ru.yandex.complaints.model._
import ru.yandex.complaints.services._
import ru.yandex.complaints.watching.ProcessingState
import ru.yandex.complaints.watching.stages.InitialStage
import ru.yandex.complaints.watching.stages.initial.ModObjResolverImpl
import ru.yandex.vertis.hobo.client.HoboClient
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Success, Try}

/**
  * Specs for [[InitialStage]]
  *
  * @author s-reznick
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class InitialStageSpec
  extends WordSpec
  with Matchers
  with MockitoSupport {

  val Now = new Timestamp(getNow)
  val DefaultOffer = Offer(
    id = Plain("o12345"),
    createTime = Now,
    authorId = "a123",
    hash = "somehash"
  )
  val Stage = "InitialStage"
  val Service = "realty"

  val hoboClient = mock[HoboClient]

  Stage should {
    "do nothing if moderation object is obtained already for complaint" in {
      object LocalCountingComplaintsDao extends CountingComplaintsDao
      object LocalCountingModerationService extends CountingModerationService

      object CountingInitalStage
        extends InitialStage(
          new ModObjResolverImpl(Service, LocalCountingModerationService, hoboClient, ""),
          LocalCountingComplaintsDao)

      val state = new ProcessingState[Offer](DefaultOffer, 2 minutes)

      val result = CountingInitalStage.process(state)

      assert(result == state)
      assert(LocalCountingComplaintsDao.totalCount == 1)
      assert(LocalCountingComplaintsDao.hfcCount == 1)
      assert(LocalCountingModerationService.totalCount == 0)
    }
  }

  Stage should {
    "reschedule if moderation client call leads to exception" in {
      object LocalCountingComplaintsDao extends CountingComplaintsDao {
        override def hasFreshComplaints(offerID: OfferID): Try[Boolean] = Try {
          hfcCount += 1
          true
        }
      }
      object LocalFailingAskModerationService extends FailingAskModerationService

      object FailingInitalStage
        extends InitialStage(
          new ModObjResolverImpl(Service, LocalFailingAskModerationService, hoboClient, ""),
          LocalCountingComplaintsDao)


      val state = new ProcessingState[Offer](DefaultOffer, 20 minutes)

      val result = FailingInitalStage.process(state)

      assert(result.elem == state.elem)
      assert(result.delay == (5 minutes))
      assert(LocalCountingComplaintsDao.totalCount == 1)
      assert(LocalCountingComplaintsDao.hfcCount == 1)
      assert(LocalFailingAskModerationService.totalCount == 1)
      assert(LocalFailingAskModerationService.askCount == 1)
    }
  }

  Stage should {
    "reschedule if attempt to set moderation object id leads to exception" in {
      object LocalComplaintsDao extends FailingSMOIComplaintsDao  {
        override def hasFreshComplaints(offerID: OfferID): Try[Boolean] = Try {
          hfcCount += 1
          true
        }
      }
      object LocalModerationService extends CountingModerationService

      object FailingInitalStage
        extends InitialStage(
          new ModObjResolverImpl(Service, LocalModerationService, hoboClient, ""),
          LocalComplaintsDao)

      val state = new ProcessingState[Offer](DefaultOffer, 20 minutes)

      val result = FailingInitalStage.process(state)

      assert(result.elem == state.elem)
      assert(result.delay == (5 minutes))
      assert(LocalComplaintsDao.totalCount == 2)
      assert(LocalComplaintsDao.smoiCount == 1)
      assert(LocalComplaintsDao.hfcCount == 1)
      assert(LocalModerationService.totalCount == 1)
      assert(LocalModerationService.askCount == 1)
    }
  }

  Stage should {
    "update modobj_id to one obtained via moderation client" in {
      object LocalComplaintsDao extends CountingComplaintsDao {
        override def hasFreshComplaints(offerID: OfferID)
        : Try[Boolean] = Try {
          hfcCount += 1
          true
        }
      }
      object LocalModerationService extends CountingModerationService

      object FailingInitalStage
        extends InitialStage(
          new ModObjResolverImpl(Service, LocalModerationService, hoboClient, ""),
          LocalComplaintsDao)

      val state = new ProcessingState[Offer](DefaultOffer, 1 minute)

      val result = FailingInitalStage.process(state)

      assert(LocalModerationService.totalCount == 1)
      assert(LocalModerationService.askCount == 1)

      val expectedModObjId = LocalModerationService.askModerationObject(
        ModerationObjectRequest(offerId = DefaultOffer.id, authorId = DefaultOffer.authorId)
      ).toOption.map(_.id)

      assert(expectedModObjId.isDefined)
      assert(result.delay == state.delay)
      assert(LocalComplaintsDao.totalCount == 2)
      assert(LocalComplaintsDao.smoiCount == 1)
      assert(LocalComplaintsDao.hfcCount == 1)
    }
  }

  Stage should {
    "update modobj_id for autoru sto" in {

      val AutoruStoOffer = AutoruSto("1")

      object LocalComplaintsDao extends CountingComplaintsDao {
        override def hasFreshComplaints(offerID: OfferID): Try[Boolean] = Try {
          hfcCount += 1
          true
        }
      }
      object LocalModerationService extends CountingModerationService

      object Stage
        extends InitialStage(
          new ModObjResolverImpl(Service, LocalModerationService, hoboClient, ""),
          LocalComplaintsDao)

      val state = new ProcessingState[Offer](
        DefaultOffer.copy(id = AutoruStoOffer),
        1.minute)

      val result = Stage.process(state)

      assert(LocalModerationService.totalCount == 0)
      assert(LocalModerationService.askCount == 0)
      assert(result.elem == state.elem)
      assert(result.delay == 1.minute)

      object Stage2
        extends InitialStage(
          new ModObjResolverImpl("autoru", LocalModerationService, hoboClient, ""),
          LocalComplaintsDao)

      val result2 = Stage2.process(state)

      assert(LocalModerationService.totalCount == 0)
      assert(LocalModerationService.askCount == 0)
      assert(result2.elem == state.elem)
      assert(result2.delay == 1.minute)

      LocalComplaintsDao.allComplaintsForOffer(AutoruStoOffer) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }

    }
  }


}