package ru.yandex.vertis.billing.event

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CampaignCallDao
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcCampaignEventDao.SnapshotRecord
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcCampaignCallDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.event.call.{
  CallFactMatcher,
  CallFactModifier,
  CampaignCallDaoDumper,
  RealtyCallFactAnalyzer
}
import ru.yandex.vertis.billing.event.logging.LoggedCallFactModifier
import ru.yandex.vertis.billing.model_core.Baggage.{StringFingerPrintVSBILLING1630Date => MigrationDate}
import ru.yandex.vertis.billing.model_core.BaggagePayload.EventSources
import ru.yandex.vertis.billing.model_core.CampaignCallFact.DetailedStatuses
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.event.EventContext
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.settings.RealtyComponents
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.granulate
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.Iterable
import scala.concurrent.Future
import scala.util.{Failure, Random, Success}

/**
  * Spec on [[FingerprintImpl]].
  * Tests that same calls are not duplicated
  * when they match on more `fresh` show events.
  */
class CallFactReader2Spec
  extends AnyWordSpec
  with Matchers
  with JdbcSpecTemplate
  with EventsProviders
  with MockitoSupport
  with AsyncSpecBase {

  def nextInt = Random.nextInt(500)
  import concurrent.duration.DurationInt

  private val DayAfterMigration =
    DateTimeInterval.dayIntervalFrom(MigrationDate.withTimeAtStartOfDay())
  private val DayBeforeMigration = DateTimeInterval.previousDayFrom(MigrationDate)
  private val CampaignCallDao = new JdbcCampaignCallDao(campaignEventDatabase)
  private val CallProductOld = Product(Placement(CostPerCall(1L)))
  private val CallProductNew = Product(Placement(CostPerCall(20L)))
  private val Campaign = CampaignHeaderGen.next

  private val CampaignWithOldProduct =
    Campaign.copy(product = CallProductOld)

  private val CampaignWithNewProduct =
    Campaign.copy(product = CallProductNew)

  private val OfferId = UserOfferIdGen.next

  private val BeforeFingerPrintMigration = {
    teleponyCallFactGen(TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Redirect))
      .next(10)
      .map(f =>
        f.withTimestamp(MigrationDate.minusMinutes(nextInt))
          .withDuration(100.seconds)
      )
      .toList
  }

  private val AfterFingerPrintMigration =
    teleponyCallFactGen(TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Redirect))
      .next(10)
      .map(f =>
        f.withTimestamp(MigrationDate.plusMinutes(nextInt))
          .withDuration(100.seconds)
      )
      .toList

  private def callFactMatchedOnOldShows(facts: Iterable[CallFact]) = facts.map { fact =>
    val payload = BaggagePayload.CallWithResolution(
      fact,
      fact.timestamp.minusDays(1).minusMinutes(nextInt),
      1000L,
      ResolutionsVector(),
      EventSources.PhoneShows
    )
    Baggage(
      fact.timestamp,
      CampaignWithOldProduct,
      OfferId.user,
      EventTypes.CallsRevenue,
      BaggageObjectId.Empty,
      payload,
      None,
      EventContext()
    )
  }

  private def callFactMatchedOnNewShows(facts: Iterable[CallFact]) = facts.map { fact =>
    val payload = BaggagePayload.CallWithResolution(
      fact,
      fact.timestamp.plusMinutes(nextInt),
      2000L,
      ResolutionsVector(),
      EventSources.PhoneShows
    )
    Baggage(
      fact.timestamp,
      CampaignWithNewProduct,
      OfferId.user,
      EventTypes.CallsRevenue,
      BaggageObjectId.Empty,
      payload,
      None,
      EventContext()
    )
  }

  private def callWithStatus(facts: Iterable[CallFact]) = facts.map { fact =>
    val payload = BaggagePayload.CallWithStatus(
      fact,
      fact.timestamp.minusMinutes(nextInt),
      2000L,
      CampaignCallFact.Statuses.Ok,
      EventSources.PhoneShows,
      ResolutionsVector(),
      DetailedStatuses.Ok
    )
    Baggage(
      fact.timestamp,
      CampaignWithNewProduct,
      OfferId.user,
      EventTypes.CallsRevenue,
      BaggageObjectId.Empty,
      payload,
      None,
      EventContext()
    )
  }

  private def expectedSnapshot(baggages: Iterable[Baggage]) =
    baggages.map(b => b.snapshot.copy(time = granulate(b.snapshot.time)))

  def mockCallFactMatcher(output: Iterable[Baggage]): CallFactMatcher = {
    val mockCallFactMatcher = mock[CallFactMatcher]
    when(mockCallFactMatcher.read(?)).thenReturn(Future.successful(output))
    mockCallFactMatcher
  }

  def getModifier(input: Iterable[Baggage]) =
    new CallFactModifier(
      mockCallFactMatcher(input),
      CampaignCallDao,
      Some(new RealtyCallFactAnalyzer(RealtyComponents))
    ) with CampaignCallDaoDumper with LoggedCallFactModifier {
      override def campaignCallDao: CampaignCallDao = CampaignCallDao
    }

  def addCalls(fact: Iterable[CallFact], interval: DateTimeInterval) = {
    getModifier(callFactMatchedOnNewShows(fact)).readAndProcess(interval).futureValue
  }

  def snapshotId(b: Baggage) =
    SnapshotRecord(b.snapshot).id

  "CallFactReader" should {

    "update call which matched on new shows" in {
      addCalls(AfterFingerPrintMigration, DayAfterMigration)

      CampaignCallDao.read(Iterable(Campaign.id), DayAfterMigration) match {
        case Success(ccf) =>
          ccf should have size (AfterFingerPrintMigration.size)

          val snapshots =
            expectedSnapshot(callWithStatus(AfterFingerPrintMigration))
          ccf.map(_.snapshot) should contain theSameElementsAs snapshots
        case Failure(exception) => throw exception
      }
    }

  }

}
