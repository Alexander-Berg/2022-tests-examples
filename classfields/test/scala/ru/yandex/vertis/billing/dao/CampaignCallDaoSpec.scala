package ru.yandex.vertis.billing.dao

import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CampaignCallDao.Filter
import ru.yandex.vertis.billing.dao.CampaignCallDao.Filter.LastCall
import ru.yandex.vertis.billing.dao.CampaignCallDaoSpec.{withCampaign, withoutProductTag, TestContext}
import ru.yandex.vertis.billing.model_core.gens.{
  campaignCallGen,
  CampaignCallGenParams,
  Producer,
  TeleponyCallFactGenCallTypes
}
import ru.yandex.vertis.billing.model_core.{
  CallFactHeader,
  CampaignCallFact,
  CampaignId,
  ExtendedCampaignCallFact,
  IncomingForCampaign,
  TeleponyCallFact
}
import ru.yandex.vertis.billing.service.CallsResolutionService.ByIds
import ru.yandex.vertis.billing.service.CampaignService.CallFilter
import ru.yandex.vertis.billing.util.{DateTimeInterval, Page}
import ru.yandex.vertis.billing.util.DateTimeUtils.DateTimeAreOrdered

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

/**
  * Specs on [[CampaignCallDao]]
  */
trait CampaignCallDaoSpec extends AnyWordSpec with Matchers with TryValues with AsyncSpecBase {

  protected def campaignCallDao: CampaignCallDao
  protected def callDao: CallFactDao

  "CampaignCallDao" should {
    val count = 10

    val interval = DateTimeInterval.currentDay
    val intervalIncomings = DateTimeInterval(interval.from.minusDays(7), interval.to)
    val slice = Page(0, count)

    val campaign1 = "Campaign 1"
    val campaign2 = "Campaign 2"

    var Context = TestContext()

    val RedirectFacts = campaignCallGen(
      CampaignCallGenParams().withCallType(TeleponyCallFactGenCallTypes.Redirect)
    ).next(count)
      .filter(f => interval.contains(f.snapshot.time))
      .map(withoutProductTag)

    val CallbackFacts = campaignCallGen(
      CampaignCallGenParams().withCallType(TeleponyCallFactGenCallTypes.Callback)
    ).next(count)
      .filter(f => interval.contains(f.snapshot.time))
      .map(withoutProductTag)

    val Facts = RedirectFacts ++ CallbackFacts

    val Facts1 = Facts.take(count / 2).map(withCampaign(campaign1))
    val Facts2 = Facts.drop(count / 2).map(withCampaign(campaign2))

    callDao.upsert((Facts1 ++ Facts2).map(_.fact)).get

    "get empty" in {
      campaignCallDao.read(campaign1, interval, slice) match {
        case Success(result) if result.iterator.isEmpty => ()
        case Failure(ex) => fail(s"Unexpected error: $ex:" + ex.getStackTrace.mkString("\n"))
        case other => fail(s"Unexpected $other")
      }
      val result = campaignCallDao.getValuableIncomings(intervalIncomings).futureValue
      result shouldBe empty
    }

    "add facts" in {
      campaignCallDao.write(Facts1).success.value

      Context = Context + Facts1
      val extendedCallFacts = campaignCallDao.read(campaign1, interval, slice).get
      extendedCallFacts
        .map(ExtendedCampaignCallFact.asCampaignCall) should contain theSameElementsAs Context.facts

      val incomingsFromFuture = campaignCallDao.getValuableIncomings(intervalIncomings).futureValue
      incomingsFromFuture should contain theSameElementsAs Context.incomings
    }

    "get empty for non-exist campaign" in {
      campaignCallDao.read(campaign2, interval, slice) match {
        case Success(result) if result.iterator.isEmpty => ()
        case other => fail(s"Unexpected $other")
      }
    }

    "repeat facts" in {
      campaignCallDao.write(Facts1) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      campaignCallDao.read(campaign1, interval, slice) match {
        case Success(result) =>
          result.map(ExtendedCampaignCallFact.asCampaignCall) should contain theSameElementsAs Context.facts
        case other => fail(s"Unexpected $other")
      }

      val res = campaignCallDao.getValuableIncomings(intervalIncomings).futureValue
      res should contain theSameElementsAs Context.incomings
    }
    "read facts with no filters" in {
      campaignCallDao.read(campaign1, interval, slice, List()) match {
        case Success(result) =>
          val expected = Context.facts
          result.map(ExtendedCampaignCallFact.asCampaignCall) should contain theSameElementsAs expected
        case other => fail(s"Unexpected $other")
      }
    }
    "read facts with custom duration" in {
      val durations = Iterable(5.seconds, 10.seconds, 20.seconds)
      durations.foreach { duration =>
        campaignCallDao.read(campaign1, interval, slice, Seq(CallFilter.CallDuration(duration))) match {
          case Success(result) =>
            val expected = Context.facts.filter(_.fact.duration >= duration)
            result.map(ExtendedCampaignCallFact.asCampaignCall) should contain theSameElementsAs expected
          case other => fail(s"Unexpected $other")
        }
      }
    }

    "read facts with durations and statuses" in {
      val durations = Iterable(5.seconds, 10.seconds, 20.seconds)
      val statuses = Iterable(CampaignCallFact.Statuses(0), CampaignCallFact.Statuses(4))
      durations.foreach { duration =>
        statuses.foreach { status =>
          campaignCallDao.read(
            campaign1,
            interval,
            slice,
            Seq(CallFilter.CallDuration(duration), CallFilter.CallStatus(status))
          ) match {
            case Success(result) =>
              val expected = Context.facts.filter(_.fact.duration >= duration).filter(_.status == status)
              result.map(ExtendedCampaignCallFact.asCampaignCall) should contain theSameElementsAs expected
            case other => fail(s"Unexpected $other")
          }
        }
      }
    }

    "batch read by campaigns" in {
      campaignCallDao.write(Facts2) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }

      Context = Context + Facts2

      campaignCallDao.get(Filter.ForCampaigns(Iterable(campaign1, campaign2), interval)) match {
        case Success(result) =>
          result should contain theSameElementsAs Context.facts
        case other => fail(s"Unexpected $other")
      }
    }

    "get empty result on empty batch" in {
      campaignCallDao.get(Filter.ForCampaigns(Set.empty, DateTimeInterval.currentDay)) match {
        case Success(list) if list.isEmpty => ()
        case other => fail(s"Unexpected $other")
      }
    }

    "get last call" in {
      val count = 5
      val campaignCalls = campaignCallGen().next(count)
      campaignCallDao.write(campaignCalls).success.value

      val lastCall = (Facts1 ++ Facts2 ++ campaignCalls).maxBy(_.fact.timestamp)

      campaignCallDao.get(LastCall) match {
        case Success(Seq(campaignCall)) =>
          campaignCall shouldBe lastCall
        case other => fail(s"Unexpected $other")
      }
    }

    "read call by ids" in {
      val count = 5
      val campaignCalls = campaignCallGen().next(count)
      val calls = campaignCalls.map(_.fact)
      callDao.upsert(calls).success.value
      campaignCallDao.write(campaignCalls).success.value

      val expected = campaignCalls.map { call =>
        val evaluated = callDao.getT(ByIds(CallFactHeader(call.fact).identity)).get.head
        ExtendedCampaignCallFact(call, evaluated)
      }
      val ids = calls.map(CallFactHeader(_).identity).toSet

      val result = campaignCallDao.getExtendedCampaignCallFact(ids).get
      result.size shouldBe count
      result should contain theSameElementsAs expected

    }

    "upsert record id" in {
      val (campaignCallWithoutRecord, campaignCallWithRecord) = {
        val call = campaignCallGen().next
        val teleponyCall = call.fact.asInstanceOf[TeleponyCallFact]
        (
          call.copy(fact = teleponyCall.copy(recordId = None)),
          call.copy(fact = teleponyCall.copy(recordId = Some("test_record")))
        )
      }
      callDao.upsert(Iterable(campaignCallWithRecord.fact)).success.value

      campaignCallDao.write(Iterable(campaignCallWithoutRecord)).success.value

      campaignCallDao.write(Iterable(campaignCallWithRecord)).success.value

      val extendedCalls = campaignCallDao.getExtendedCampaignCallFact(Set(campaignCallWithRecord.fact.id)).get
      extendedCalls.size shouldBe 1
      extendedCalls.head.fact.call.asInstanceOf[TeleponyCallFact].recordId should not be empty
    }

  }

}

object CampaignCallDaoSpec {

  case class TestContext(facts: Iterable[CampaignCallFact] = Iterable.empty) {

    def incomings = facts.map(f => IncomingForCampaign(f.snapshot.campaignId, f.fact.incoming)).toSet

    def +(add: Iterable[CampaignCallFact]): TestContext =
      TestContext(facts ++ add)
  }

  def withCampaign(campaign: CampaignId)(fact: CampaignCallFact) =
    fact.copy(snapshot = fact.snapshot.copy(campaignId = campaign))

  def withoutProductTag(fact: CampaignCallFact) =
    fact.copy(snapshot = fact.snapshot.copy(product = fact.snapshot.product.copy(tag = None)))

}
