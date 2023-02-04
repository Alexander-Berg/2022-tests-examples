package ru.yandex.vertis.billing.dao

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterEach}
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CallsSearchDao.{Filter, Record, SearchParameters, Value}
import ru.yandex.vertis.billing.model_core.BaggagePayload.{EventSources, PhoneShowIdentifier}
import ru.yandex.vertis.billing.model_core.gens.{campaignCallGen, CampaignCallGenParams, TeleponyCallFactGenCallTypes}
import ru.yandex.vertis.billing.model_core.{notNull, CampaignCallFact}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.{Failure, Success}

/**
  * @author ruslansd
  */
trait CallsSearchDaoBaseSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with AsyncSpecBase {

  protected def callRequestsDaoFactory: CallsSearchDaoFactory

  var callsSearchDao: CallsSearchDao = null

  val RedirectCampaignCallForPhoneShowGen: Gen[CampaignCallFact] = campaignCallGen(
    CampaignCallGenParams(source = Set(EventSources.PhoneShows)).withCallType(TeleponyCallFactGenCallTypes.Redirect)
  )

  val RedirectCampaignCallForCampaignHistoryGen: Gen[CampaignCallFact] = campaignCallGen(
    CampaignCallGenParams(source = Set(EventSources.CampaignHistory))
      .withCallType(TeleponyCallFactGenCallTypes.Redirect)
  )

  val CallbackCampaignCallForPhoneShowGen: Gen[CampaignCallFact] = campaignCallGen(
    CampaignCallGenParams(source = Set(EventSources.PhoneShows)).withCallType(TeleponyCallFactGenCallTypes.Callback)
  )

  val CallbackCampaignCallForCampaignHistoryGen: Gen[CampaignCallFact] = campaignCallGen(
    CampaignCallGenParams(source = Set(EventSources.CampaignHistory))
      .withCallType(TeleponyCallFactGenCallTypes.Callback)
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    if (notNull(callsSearchDao)) callsSearchDao.cleanup().get
    callsSearchDao = callRequestsDaoFactory.instance().futureValue
  }

  def successWrite(records: Iterable[Record]): Unit = {
    callsSearchDao.write(records) match {
      case Success(_) => ()
      case Failure(exception) => fail(s"Unexpected failure", exception)
    }
  }

  def successGetNothing(filter: Filter): Unit = {
    callsSearchDao.get(filter) match {
      case Success(None) => ()
      case Success(other) => fail(s"Unexpected success result $other")
      case Failure(exception) => fail(s"Unexpected failure", exception)
    }
  }

  def successGetSomething(filter: Filter)(check: Value => Assertion): Assertion = {
    callsSearchDao.get(filter) match {
      case Success(Some(r)) => check(r)
      case Success(other) => fail(s"Unexpected success result $other")
      case Failure(exception) => fail(s"Unexpected failure", exception)
    }
  }

  def asRecord(f: CampaignCallFact): Record = {
    val time = f.fact.timestamp
    val identifier = PhoneShowIdentifier(f.fact.objectId, f.fact.redirect, f.fact.tag)
    Record(SearchParameters(time, identifier, f.source), serialize(f))
  }

  def recordWith(time: DateTime, fact: CampaignCallFact)(record: Record) =
    record.copy(parameters = record.parameters.copy(time = time), value = serialize(fact))

  def serialize(f: CampaignCallFact): Value = {
    val builder = new StringBuilder()
    builder
      .append(f.snapshot.fingerprint.value)
      .append(f.revenue)
      .append(f.source.toString)
    builder.toString().getBytes
  }

  def shiftTime(shift: FiniteDuration = 10.minutes)(record: Record) = {
    val time = record.parameters.time.minus(shift.toMillis)
    record.copy(parameters = record.parameters.copy(time = time))
  }

}
