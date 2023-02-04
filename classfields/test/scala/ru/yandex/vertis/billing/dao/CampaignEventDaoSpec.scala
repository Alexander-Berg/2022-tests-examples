package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CampaignEventDaoSpec.{rough, withoutProductFingerprint}
import ru.yandex.vertis.billing.model_core.{CampaignEvents, EventStat, FingerprintImpl}
import ru.yandex.vertis.billing.model_core.EventStat.RevenueDetails
import ru.yandex.vertis.billing.model_core.gens.{CampaignEventsGen, EventStatRevenueDetailsGen, Producer}
import ru.yandex.vertis.billing.dao.CampaignEventDao.Filter
import ru.yandex.vertis.billing.dao.CampaignEventDao.Filter.OnCampaign
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.{now, today, DateTimeAreOrdered}
import ru.yandex.vertis.billing.util.TestingHelpers.RichOrderCampaignEvents

import scala.util.Success

/**
  * Specs on [[CampaignEventDao]]
  *
  * @author dimas
  */
trait CampaignEventDaoSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  protected def campaignEventDao: CampaignEventDao

  private def readSingle() =
    campaignEventDao.read(0).get.head

  "CampaignEventDao" should {
    val event = CampaignEventsGen.next

    "write event initially" in {
      campaignEventDao.writeT(event).get
    }

    "write same event without modify epoch" in {
      val initial = readSingle()
      campaignEventDao.writeT(event).get
      val updated = readSingle()
      updated should be(initial)
    }

    "write updated event and inc epoch" in {
      val initial = readSingle()

      val update = event.copy(stat = event.stat.copy(value = event.stat.value + 10))

      campaignEventDao.writeT(update).get

      val updated = readSingle()

      updated.snapshot should be(initial.snapshot)
      updated.stat.value should be(initial.stat.value + 10)
      assert(updated.stat.epoch.get > initial.stat.epoch.get)
    }

    val events = CampaignEventsGen.next(10) ++
      Iterable(event)

    "write campaign events" in {
      campaignEventDao.writeT(events).get
    }

    "write campaign events with Future function" in {
      campaignEventDao.write(events).futureValue
    }

    "read all campaign events" in {
      val retrieved = campaignEventDao.read(0).get
      retrieved.map(rough).toSet should
        be(events.map(rough).toSet)
    }

    "read particular campaign events" in {
      val campaignId = events.head.snapshot.campaignId
      val forCampaign = events.filter { e =>
        e.snapshot.campaignId == campaignId
      }

      val minTime = events.minBy(e => e.snapshot.time).snapshot.time
      val maxTime = events.maxBy(e => e.snapshot.time).snapshot.time
      val interval = DateTimeInterval(minTime, maxTime)

      val filter = Filter.OnCampaign(campaignId, interval)

      val retrieved = campaignEventDao.read(filter).get
      retrieved.map(withoutProductFingerprint).map(rough).toSet should
        be(forCampaign.map(withoutProductFingerprint).map(rough).toSet)
    }

    "write too many campaign events" in {
      campaignEventDao.writeT(CampaignEventsGen.next(1000)).get
      campaignEventDao.read(now().minusMinutes(5).getMillis).get.size should be > 100
    }

    "write hourly ranged events" in {
      val start = now()

      val events = (0 to 23).map(h => {
        val e = CampaignEventsGen.next
        e.copy(
          e.snapshot.copy(campaignId = "6", time = start.withTimeAtStartOfDay().plusHours(h)),
          e.stat.copy(value = 2000)
        )
      })
      campaignEventDao.writeT(events).get

      val retrieved = campaignEventDao.read(start.getMillis).get
      retrieved.map(_.withoutEpoch).map(withoutProductFingerprint) should
        contain theSameElementsAs events.map(_.withoutEpoch).map(withoutProductFingerprint)
    }

    "write events with writeEpoch=true" in {
      val start = now()
      val events = (0 to 10).map(h => {
        val e = CampaignEventsGen.next
        e.copy(
          e.snapshot.copy(campaignId = "7", time = start.withTimeAtStartOfDay().plusHours(h)),
          e.stat.copy(value = 2000)
        )
      })
      intercept[IllegalArgumentException] {
        campaignEventDao.writeT(events, writeEpoch = true).get
      }

      // order epoch by hour below
      val withEpoch = events.map(e =>
        e.copy(stat = e.stat.copy(epoch = Some(start.plusMinutes(2 + e.snapshot.time.getHourOfDay).getMillis)))
      )
      campaignEventDao.writeT(withEpoch, writeEpoch = true).get
      val retrieved = campaignEventDao.read(start.plusMinutes(1).getMillis).get
      withEpoch.map(_.withoutEpoch).map(withoutProductFingerprint) should
        be(retrieved.map(_.withoutEpoch).map(withoutProductFingerprint))
    }

    "write events with details" in {
      val events = (1 to 100).map(h => {
        val e = CampaignEventsGen.next
        e.copy(
          snapshot = e.snapshot.copy(campaignId = "8"),
          stat = e.stat.copy(value = h, details = Some(RevenueDetails(Map(s"offer$h" -> h))))
        )
      })
      campaignEventDao.writeT(events).get
      campaignEventDao.read(OnCampaign("8", today())) match {
        case Success(it) if it.nonEmpty =>
          it.size should be <= 100
          it.foreach { r =>
            r.stat.details match {
              case Some(RevenueDetails(map)) if map.size == 1 =>
                map.get(s"offer${r.stat.value}") should be(Some(r.stat.value))
              case other => fail(s"Unexpected $other")
            }
          }
        case other => fail(s"Unexpected $other")
      }
    }

    "write events with heavy details" in {
      val Campaign = "9"

      val events = (1 to 1000).map(h => {
        val e = CampaignEventsGen.next
        e.copy(
          snapshot = e.snapshot.copy(campaignId = Campaign),
          stat = e.stat.copy(value = h, details = Some(EventStatRevenueDetailsGen.next))
        )
      })
      campaignEventDao.write(events).futureValue
      campaignEventDao.read(OnCampaign(Campaign, today())) match {
        case Success(it) if it.nonEmpty =>
          it.size should be <= 1000
          it.foreach { r =>
            r.stat.details match {
              case Some(RevenueDetails(_)) => ()
              case other => fail(s"Unexpected $other")
            }
          }
          it.collect {
            case CampaignEvents(_, EventStat(_, _, _, Some(RevenueDetails(map)), _, _)) if map.nonEmpty => map
          } should not be empty
        case other => fail(s"Unexpected $other")
      }
    }

  }
}

object CampaignEventDaoSpec {

  private def withoutProductFingerprint(events: CampaignEvents): CampaignEvents =
    CampaignEvents(events.snapshot.copy(fingerprint = FingerprintImpl("")), events.stat)

  private def rough(events: CampaignEvents): CampaignEvents = {
    val hours = events.snapshot.time.getHourOfDay
    val roughTime = events.snapshot.time.withTimeAtStartOfDay().plusHours(hours)
    val roughSnapshot = events.snapshot.copy(time = roughTime)
    val roughStat = events.stat.copy(epoch = None)
    CampaignEvents(roughSnapshot, roughStat)
  }
}
