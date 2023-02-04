package ru.yandex.vertis.telepony.dao

import org.joda.time.{DateTime, Days, Years}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{CallResults, Operators, RawCall}
import ru.yandex.vertis.telepony.service.UnmatchedCallService.UnmatchedFilter
import ru.yandex.vertis.telepony.util.Range

import scala.concurrent.Future

/**
  * @author evans
  */
trait UnmatchedRawCallDaoSpec extends SpecBase with BeforeAndAfterEach {

  def dao: UnmatchedRawCallDao

  import scala.concurrent.ExecutionContext.Implicits._

  override protected def beforeEach(): Unit = {
    dao.clear().futureValue
    super.beforeEach()
  }

  private val callGen: Gen[RawCall] =
    RawCallGen.map(
      _.copy(
        callResult = CallResults.Unknown,
        origin = RawCall.Origins.Offline,
        operator = Operators.Mts,
        fallbackCall = None
      )
    )

  "Unmatched dao" should {
    "upsert" in {
      val call = callGen.next
      dao.createOrUpdate(call).futureValue
      dao.exists(call.externalId).futureValue should ===(true)
    }

    "list by proxy" in {
      val phone = PhoneGen.next
      val calls = callGen.next(5).map(_.copy(proxy = phone))
      Future.sequence(calls.map(dao.createOrUpdate)).futureValue
      val filter = UnmatchedFilter.ByProxy(phone, new DateTime(0L), DateTime.now().plusYears(1000))
      val slice = Range.Full
      dao.list(filter, slice).futureValue.toSet shouldEqual calls.toSet
    }
    "list by source" in {
      val source = RefinedSourceGen.next
      val calls = callGen.next(5).map(_.copy(source = Some(source)))
      Future.sequence(calls.map(dao.createOrUpdate)).futureValue
      val filter = UnmatchedFilter.BySource(source, new DateTime(0L), DateTime.now().plusYears(1000))
      val slice = Range.Full
      dao.list(filter, slice).futureValue.toSet shouldEqual calls.toSet
    }
    "upsert update" in {
      val call = callGen.next
      dao.createOrUpdate(call).futureValue
      val updatedCall = callGen.next.copy(externalId = call.externalId, proxy = call.proxy)
      dao.createOrUpdate(updatedCall).futureValue
      val filter =
        UnmatchedFilter.ByProxy(call.proxy, new DateTime(0L), DateTime.now().plusYears(1000))
      val slice = Range.Full
      dao.list(filter, slice).futureValue.toSet shouldEqual Set(updatedCall)
    }
  }

  it when {
    "listUniqueCallers is called" should {
      "respect threshold" in {
        val Array(source1, source2) = RefinedSourceGen.nextUnique(2).toArray
        val targets1 = PhoneGen.nextUnique(7)
        val targets2 = PhoneGen.nextUnique(6)

        callGen
          .next(7)
          .zip(targets1)
          .map {
            case (rawCall, phone) =>
              rawCall.copy(source = Option(source1), proxy = phone, startTime = DateTime.now().minusMonths(1))
          }
          .foreach(dao.createOrUpdate(_).futureValue)

        RawCallGen
          .next(6)
          .zip(targets2)
          .map {
            case (rawCall, phone) =>
              rawCall.copy(source = Option(source2), proxy = phone, startTime = DateTime.now().minusMonths(1))
          }
          .foreach(dao.createOrUpdate(_).futureValue)

        val callers = dao.listUniqueCallers(Years.THREE, 6).futureValue.toIndexedSeq
        assert(callers.size == 1)
        assert(callers(0) == source1)
      }

      "inspect specified period of time" in {
        val sources = RefinedSourceGen.nextUnique(2).toIndexedSeq

        callGen
          .next(2)
          .zip(sources)
          .zip(List(Days.days(21), Days.days(19)))
          .map {
            case ((rawCall, source), timeAgo) =>
              rawCall.copy(source = Option(source), startTime = DateTime.now().minus(timeAgo))
          }
          .foreach(dao.createOrUpdate(_).futureValue)

        val callers = dao.listUniqueCallers(Days.days(20), 0).futureValue.toIndexedSeq
        assert(callers.size == 1)
        assert(callers(0) == sources(1))
      }
    }
  }
}
