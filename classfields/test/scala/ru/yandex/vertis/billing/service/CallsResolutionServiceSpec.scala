package ru.yandex.vertis.billing.service

import java.util.NoSuchElementException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.service.impl.CallsResolutionServiceImpl
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.billing.model_core.gens.{
  campaignCallGen,
  teleponyCallFactGen,
  CallComplaintGen,
  CampaignCallGenParams,
  Producer,
  TeleponyCallFactGenCallTypes,
  TeleponyCallFactGenParams
}
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcCallFactDao, JdbcCampaignCallDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.model_core.{CallFact, CallFactHeader, CallFactId, Resolution, TeleponyCallFact}
import ru.yandex.vertis.billing.model_core.Resolution.{Automatic, Manually}
import ru.yandex.vertis.billing.service.CallsResolutionService.{ByIds, Patch}
import ru.yandex.vertis.billing.service.impl.CallsResolutionServiceImpl.ComplaintAlreadyExists
import ru.yandex.vertis.billing.util.AutomatedContext

import scala.util.{Failure, Success}

class CallsResolutionServiceSpec extends AnyWordSpec with Matchers with MockitoSupport with JdbcSpecTemplate {

  private val callFactDao = new JdbcCallFactDao(campaignEventDualDatabase)
  private val campaignCallDao = new JdbcCampaignCallDao(campaignEventDualDatabase.master)

  private def callFactInfo(): (CallFact, CallFactId) = {
    val callFact = teleponyCallFactGen().next
    (callFact, callFact.id)
  }

  private val complain = CallComplaintGen.next

  private val PassedManuallyResolution = Manually(Resolution.Statuses.Pass)
  private val FailedManuallyResolution = Manually(Resolution.Statuses.Fail)
  private val AutomaticResolution = Automatic(Resolution.Statuses.Fail)

  implicit private val oc = AutomatedContext("test")

  private val callResolutionService = new CallsResolutionServiceImpl(callFactDao, campaignCallDao)

  "CallsResolutionService" should {

    "process add complain correctly" in {
      val (callFact, callFactId) = callFactInfo()

      callResolutionService.complain(callFactId, complain) match {
        case Failure(_: NoSuchElementException) => ()
        case Failure(exception) => fail("should fail with NoSuchElementException", exception)
        case Success(_) => fail("should fail")
      }

      callFactDao.upsert(Seq(callFact)).get

      callResolutionService.complain(callFactId, complain) match {
        case Success(_) => ()
        case Failure(exception) => fail("should succeed", exception)
      }

      callFactDao.getT(ByIds(callFactId)) match {
        case Success(c) =>
          c.size shouldBe 1
          c.head.complaint.isDefined shouldBe true
          c.head.complaint.get shouldBe complain
        case Failure(exception) =>
          fail("should succeed", exception)
      }

      callResolutionService.complain(callFactId, complain) match {
        case Failure(_: ComplaintAlreadyExists) => ()
        case Failure(exception) => fail("should fail with ComplaintAlreadyExists", exception)
        case Success(_) => fail("should fail")
      }
    }

    "fail add complain with automatic resolution" in {
      val (callFact, callFactId) = callFactInfo()

      callFactDao.upsert(Seq(callFact)).get

      callFactDao.update(callFactId, Patch(resolution = Some(AutomaticResolution))) shouldBe Success(())

      callResolutionService.complain(callFactId, complain) match {
        case Failure(_: IllegalArgumentException) => ()
        case Failure(exception) => fail("should fail with IllegalArgumentException", exception)
        case Success(_) => fail("should fail")
      }

      callFactDao.update(callFactId, Patch(resolution = Some(PassedManuallyResolution))) shouldBe Success(())

      callResolutionService.complain(callFactId, complain) match {
        case Failure(_: IllegalArgumentException) => ()
        case Failure(exception) => fail("should fail with IllegalArgumentException", exception)
        case Success(_) => fail("should fail")
      }
    }

    "fail add complain with failed manual resolution" in {
      val (callFact, callFactId) = callFactInfo()

      callFactDao.upsert(Seq(callFact)).get

      callFactDao.update(callFactId, Patch(resolution = Some(FailedManuallyResolution))) shouldBe Success(())

      callResolutionService.complain(callFactId, complain) match {
        case Failure(_: IllegalArgumentException) => ()
        case Failure(exception) => fail("should fail with IllegalArgumentException", exception)
        case Success(_) => fail("should fail")
      }
    }

    "process add complain with passed manual resolution" in {
      val callFact = teleponyCallFactGen(TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Redirect)).next match {
        case t: TeleponyCallFact =>
          t.copy(timestamp = CallFact.HoboTaskNewLogicVSBILLING3396StarDate.plus(1000))
        case other =>
          fail(s"Unexpected $other")
      }
      val callFactId = callFact.id

      callFactDao.upsert(Seq(callFact)).get

      callFactDao.update(callFactId, Patch(resolution = Some(PassedManuallyResolution))) shouldBe Success(())

      callResolutionService.complain(callFactId, complain) match {
        case Success(_) => ()
        case Failure(exception) => fail("should succeed", exception)
      }
    }

    "fail add complain with call before 2019-06-20 12:00:00 +03:00" in {
      val callFact = teleponyCallFactGen(TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Redirect)).next match {
        case t: TeleponyCallFact =>
          t.copy(timestamp = CallFact.HoboTaskNewLogicVSBILLING3396StarDate.minus(1000))
        case other =>
          fail(s"Unexpected $other")
      }
      val callFactId = callFact.id

      callFactDao.upsert(Seq(callFact)).get

      callFactDao.update(callFactId, Patch(resolution = Some(PassedManuallyResolution))) shouldBe Success(())

      callResolutionService.complain(callFactId, complain) match {
        case Failure(_: IllegalArgumentException) => ()
        case Failure(exception) => fail("should fail with IllegalArgumentException", exception)
        case Success(_) => fail("should fail")
      }
    }

    "fail add complain for used car's call by price <= 100 kopeks" in {
      val carUsedCampaignCall =
        campaignCallGen(CampaignCallGenParams().withTag(Some("section=USED"))).next.copy(revenue = 100)

      callFactDao.upsert(Seq(carUsedCampaignCall.fact)).get
      campaignCallDao.write(Seq(carUsedCampaignCall)).get

      callResolutionService.complain(carUsedCampaignCall.fact.id, complain) match {
        case Failure(ex: IllegalArgumentException) =>
          ex.getMessage.contains("Can't set complaint for used car's call with price") shouldBe true
        case Failure(exception) => fail("should fail with specific IllegalArgumentException", exception)
        case Success(_) => fail("should fail")
      }
    }

    "can add complain for used car's call by price > 100 kopeks" in {
      val carUsedCampaignCall =
        campaignCallGen(CampaignCallGenParams().withTag(Some("section=USED"))).next.copy(revenue = 101)

      callFactDao.upsert(Seq(carUsedCampaignCall.fact)).get
      campaignCallDao.write(Seq(carUsedCampaignCall)).get

      callResolutionService.complain(carUsedCampaignCall.fact.id, complain).get
    }

  }

}
