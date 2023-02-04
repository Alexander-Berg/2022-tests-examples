package ru.yandex.vertis.billing.dao.impl.jdbc

import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.Filter.{
  InIntervalAndLastBefore,
  UpdatedSinceBatchOrdered,
  WithEventTypeBatched
}
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.{CampaignHistoryPoint, EventTypes}
import ru.yandex.vertis.billing.dao.gens.{campaignStateChangeGen, CampaignStateChangeGenParams}
import ru.yandex.vertis.billing.model_core.gens.{CampaignIdGen, DateTimeGen, EpochGen, Producer}
import ru.yandex.vertis.billing.model_core.{gens, CampaignId, Epoch}
import ru.yandex.vertis.billing.util.clean.CleanableCampaignHistoryDao
import ru.yandex.vertis.billing.util.{DateTimeInterval, DateTimeUtils}

import scala.util.{Failure, Success}

/**
  * Spec on [[ru.yandex.vertis.billing.dao.CampaignHistoryDao]]
  *
  * @author ruslansd
  */
class CampaignHistoryDaoSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate with AsyncSpecBase {

  private val dao = new JdbcCampaignHistoryDao(billingDatabase) with CleanableCampaignHistoryDao

  private val campaignsCount = 10
  private val campaignIds = CampaignIdGen.next(campaignsCount)

  private val MaxStatesPerCampaignCount = 20

  private val historyPointsMap = campaignIds.map { id =>
    val params = CampaignStateChangeGenParams(Some(id), Some(MaxStatesPerCampaignCount))
    val states = campaignStateChangeGen(params).next
    id -> states
  }.toMap

  private def withoutCreatTimestamp(point: CampaignHistoryPoint): CampaignHistoryPoint = {
    val header = point.header.copy(createTimestamp = None)
    point.copy(header = header)
  }

  private val historyPointsMapWithoutTimestamps = historyPointsMap.view.mapValues { values =>
    values.map(withoutCreatTimestamp)
  }.toMap

  private val historyPointsWithoutCreateTimestamp = historyPointsMapWithoutTimestamps.values.flatten
  private val headers = historyPointsMap.values.flatten.map(_.header)
  private val sortedHeaders = headers.toSeq.sortBy(_.epoch)
  private val headersEpoches = sortedHeaders.flatMap(_.epoch)
  private val lessThanAnyHeadersEpoch = headersEpoches.head - 1
  private val greaterThanAnyHeadersEpoch = headersEpoches.last + 1

  private def dateIntervalGen(min: Long, max: Long): Gen[DateTimeInterval] = {
    for {
      from <- Gen.choose(min, max - 1)
      to <- Gen.choose(from + 1, max)
    } yield DateTimeInterval(
      DateTimeUtils.fromMillis(from),
      DateTimeUtils.fromMillis(to)
    )
  }

  private def checkGetUpdateSinceBatchOrdered(epoch: Epoch, id: Option[CampaignId], batchSize: Int): Assertion = {
    val unsortedExpectedPoints = historyPointsWithoutCreateTimestamp.filter { point =>
      point.header.epoch.exists(_ > epoch) || point.header.epoch.contains(epoch) && id.exists(_ < point.header.id)
    }.toSeq
    val sortedExpectedPoints = unsortedExpectedPoints.sortBy(h => (h.header.epoch, h.header.id))
    val expectedPoints = sortedExpectedPoints.take(batchSize)
    dao.getTry(UpdatedSinceBatchOrdered(epoch, id, batchSize)) match {
      case Success(actualPoints) if actualPoints.isEmpty =>
        expectedPoints.isEmpty shouldBe true
      case Success(actualPoints) =>
        (actualPoints.toSeq should contain).theSameElementsInOrderAs(expectedPoints)
        val last = actualPoints.last
        val lastId = Some(last.header.id)
        val lastEpoch = last.header.epoch.get
        checkGetUpdateSinceBatchOrdered(lastEpoch, lastId, batchSize)
      case other =>
        fail(s"Unexpected $other")
    }
  }

  "CampaignHistoryDao" should {
    "get nothing when db is empty" when {
      "call get with InIntervalAndLastBefore filter" in {
        val interval = DateTimeUtils.wholeDay(DateTimeUtils.now())
        dao.getTry(InIntervalAndLastBefore(interval)) match {
          case Success(headers) if headers.isEmpty =>
          case res => fail(res.toString)
        }
      }
      "call get with InIntervalAndLastBefore filter with Future function" in {
        val interval = DateTimeUtils.wholeDay(DateTimeUtils.now())
        val res = dao.get(InIntervalAndLastBefore(interval)).futureValue
        res shouldBe empty
      }
      "call get with UpdateSinceBatch filter" in {
        val epoch = EpochGen.next
        val id = CampaignIdGen.next
        val batchSize = 1000
        dao.getTry(UpdatedSinceBatchOrdered(epoch, Some(id), batchSize)) match {
          case Success(headers) if headers.isEmpty =>
          case res => fail(res.toString)
        }
      }
      "call get with WithEventTypeBatched filter" in {
        val batchSize = 1
        EventTypes.values.foreach { eventType =>
          dao.getTry(WithEventTypeBatched(eventType, batchSize)) match {
            case Success(headers) if headers.isEmpty =>
            case res => fail(res.toString)
          }
        }
      }
    }
    "fail store" when {
      "campaign header without epoch" in {
        val withoutEpoch = headers.head.copy(epoch = None)
        dao.store(withoutEpoch) should matchPattern { case Failure(_: IllegalArgumentException) =>
        }
      }
      "campaign header without create timestamp" in {
        val headers = historyPointsWithoutCreateTimestamp.map(_.header)
        val withoutEpoch = headers.head.copy(epoch = None)
        dao.store(withoutEpoch) should matchPattern { case Failure(_: IllegalArgumentException) =>
        }
      }
    }
    "swap event type" when {
      "operate on one header" in {
        val raw = historyPointsMap.values.flatten
        val aimsCount = raw.size / 3
        val aims = raw.take(aimsCount)
        val aimHeaders = aims.map(_.header)

        aimHeaders.foreach { header =>
          dao.store(header) match {
            case Success(_) =>
            case Failure(exception) => fail(exception)
          }
        }

        val updates = aims.map { point =>
          val toType = point.eventType match {
            case EventTypes.Update => EventTypes.Create
            case EventTypes.Create => EventTypes.Update
          }
          point.copy(eventType = toType)
        }

        dao.swapTypeBatch(updates.toSeq) match {
          case Success(_) =>
          case Failure(exception) => fail(exception)
        }
        dao.getTry(UpdatedSinceBatchOrdered(0L, None, aimsCount + 1)) match {
          case Success(actual) if actual.size == aimsCount =>
            val expected = updates.map(withoutCreatTimestamp)
            actual should contain theSameElementsAs expected
          case res => fail(res.toString)
        }
      }
    }
    "store" when {
      "campaign headers with epoch were passed" in {
        // clean after swap event type
        dao.clean().get
        headers.foreach { header =>
          dao.store(header) should matchPattern { case Success(_) =>
          }
        }
      }
    }
    "get stored headers" when {
      "call get with InIntervalAndLastBefore filter" in {
        val gen = dateIntervalGen(lessThanAnyHeadersEpoch, greaterThanAnyHeadersEpoch)
        val intervalsCount = 30
        val intervals = gen.next(intervalsCount)
        intervals.foreach { interval =>
          val actualHistoryPoints = dao.get(InIntervalAndLastBefore(interval)).futureValue
          val expectedHistoryPoints = historyPointsMapWithoutTimestamps.flatMap { case (_, points) =>
            val sorted = points.toSeq.sortBy(_.header.epoch)
            val pointsInInterval = sorted.filter { point =>
              point.header.epoch.exists(_ <= interval.to.getMillis) && point.header.epoch
                .exists(_ > interval.from.getMillis)
            }
            val pointsBefore = sorted.takeWhile { point =>
              point.header.epoch.exists(_ <= interval.from.getMillis)
            }
            val lastPointBefore = pointsBefore.lastOption
            lastPointBefore
              .map { header =>
                pointsInInterval :+ header
              }
              .getOrElse(pointsInInterval)
          }
          actualHistoryPoints should contain theSameElementsAs expectedHistoryPoints
        }
      }
      "call get with UpdateSinceBatchOrdered filter" in {
        val gen = Gen.choose(lessThanAnyHeadersEpoch, greaterThanAnyHeadersEpoch)
        val epochesCount = 30
        val epoches = gen.next(epochesCount)
        val batchSize = 5
        epoches.foreach { epoch =>
          checkGetUpdateSinceBatchOrdered(epoch, None, batchSize)
        }
      }
      "call get with WithEventTypeBatched filter" in {
        val groupedByEventType = historyPointsWithoutCreateTimestamp.groupBy(_.eventType)
        groupedByEventType.foreach { case (eventType, historyPoints) =>
          val batchSize = historyPoints.size + 10
          dao.getTry(WithEventTypeBatched(eventType, batchSize)) match {
            case Success(actual) if actual.size == historyPoints.size =>
              actual should contain theSameElementsAs historyPoints
            case other =>
              fail(s"Unexpected $other")
          }
        }
      }
    }
    "store and get" when {
      "campaign header with limit were passed" in {
        dao.clean().get
        val timestamp = DateTimeGen.next
        val header =
          gens.CampaignHeaderGen.next.copy(epoch = Some(timestamp.getMillis), createTimestamp = Some(timestamp))
        val headerWithLimit = header.copy(
          settings = header.settings.copy(limit = gens.LimitGen.next)
        )

        dao.store(headerWithLimit).get
        val actual =
          dao.getTry(InIntervalAndLastBefore(dateIntervalGen(header.epoch.get - 1, header.epoch.get + 1).next)).get.head

        actual.header.settings.limit shouldBe headerWithLimit.settings.limit
      }
    }
  }

}
