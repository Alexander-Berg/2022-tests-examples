package ru.yandex.vertis.billing.dao

import org.scalacheck.Gen
import ru.yandex.vertis.billing.dao.CallsSearchDao.ForCallFactFilter
import ru.yandex.vertis.billing.model_core.BaggagePayload.EventSources
import ru.yandex.vertis.billing.model_core.gens.{campaignCallGen, Producer}
import ru.yandex.vertis.billing.model_core.CampaignCallFact
import ru.yandex.vertis.billing.util.DateTimeUtils

import scala.concurrent.duration._

/**
  * Specs on [[CampaignCallDao]]
  */
trait CallsSearchDaoSpec extends CallsSearchAutocloseableDaoBaseSpec {

  def baseNotGetCases(callFactGen: Gen[CampaignCallFact], window: FiniteDuration): Unit = {
    "for wrong time" in {
      val callFact = callFactGen.next
      val record = asRecord(callFact)

      successWrite(Seq(record))

      val defaultFilter = ForCallFactFilter(callFact.fact, window)
      val timestamp = defaultFilter.timestamp.minus(window.toMillis)
      val changedTimestamp = timestamp
      val filterForChangedTimestamp = defaultFilter.copy(timestamp = changedTimestamp)
      successGetNothing(filterForChangedTimestamp)
    }
    "for wrong object id" in {
      val callFact = callFactGen.next
      val record = asRecord(callFact)

      successWrite(Seq(record))

      val defaultFilter = ForCallFactFilter(callFact.fact, window)
      val objectId = defaultFilter.identifier.objectId
      val changedObjectId = s"$objectId:end"
      val filterForChangedObjectId =
        defaultFilter.copy(identifier = defaultFilter.identifier.copy(objectId = changedObjectId))
      successGetNothing(filterForChangedObjectId)
    }
  }

  def baseGetCases(callFactGen: Gen[CampaignCallFact], window: FiniteDuration): Unit = {
    "for same params" in {
      val callFact = callFactGen.next
      val record = asRecord(callFact)

      successWrite(Seq(record))

      successGetSomething(ForCallFactFilter(callFact.fact, window)) { r =>
        r should equal(serialize(callFact))
      }
    }
  }

  "CallRequestsDao" should {
    val window = 2.hours
    "write nothing and get nothing" in {
      successGetNothing(ForCallFactFilter(campaignCallGen().next.fact, window))
    }
    "get redirect call for phone show" when {
      behave.like(baseGetCases(RedirectCampaignCallForCampaignHistoryGen, window))
      "for correct window" in {
        val callFact = RedirectCampaignCallForPhoneShowGen.next
        val record = asRecord(callFact)
        val request = recordWith(
          DateTimeUtils.fromMillis(record.parameters.time.getMillis - window.toMillis + 1.minute.toMillis),
          callFact
        )(record)

        successWrite(Seq(request))

        val inWindow = Seq(window, window.plus(5.minute), window.plus(10.minute))
        inWindow.foreach { window =>
          successGetSomething(ForCallFactFilter(callFact.fact, window)) { r =>
            r should equal(serialize(callFact))
          }
        }
      }
      "for batch of similar calls" in {
        val callFactRevenue = 100
        val callFact = RedirectCampaignCallForPhoneShowGen.next.copy(revenue = callFactRevenue)
        val record = asRecord(callFact)

        val params = Seq(-70, 70, 10, -10, -50, 5, 1, 2, -2, -1, -5, 50, 10)

        params.foldLeft(-callFactRevenue) { case (m, p) =>
          val expectedDiff = if (p < 0) Math.max(m, p) else m
          val expectedRevenue = callFactRevenue + expectedDiff
          val request = recordWith(
            record.parameters.time.plusMinutes(p),
            callFact.copy(revenue = expectedRevenue)
          )(record)
          successWrite(Seq(request))
          successGetSomething(ForCallFactFilter(callFact.fact, window)) { r =>
            r should equal(serialize(callFact.copy(revenue = expectedRevenue)))
          }
          expectedDiff
        }
      }
    }
    "not get redirect call for phone show" when {
      behave.like(baseNotGetCases(RedirectCampaignCallForPhoneShowGen, window))
      "for wrong redirect phone" in {
        val callFact = RedirectCampaignCallForPhoneShowGen.next
        val record = asRecord(callFact)

        successWrite(Seq(record))

        val defaultFilter = ForCallFactFilter(callFact.fact, window)
        val redirectPhone = defaultFilter.identifier.redirectPhone
        val changedRedirectPhone = redirectPhone.map { redirectPhone =>
          val suffix = (redirectPhone.phone.toInt + 1).toString
          redirectPhone.copy(phone = suffix)
        }
        val filterForChangedRedirectPhone = defaultFilter.copy(
          identifier = defaultFilter.identifier.copy(redirectPhone = changedRedirectPhone)
        )
        successGetNothing(filterForChangedRedirectPhone)
      }
      "for wrong window" in {
        val callFact = RedirectCampaignCallForPhoneShowGen.next
        val record = asRecord(callFact)
        val request = recordWith(
          DateTimeUtils.fromMillis(record.parameters.time.getMillis - window.toMillis + 1.minute.toMillis),
          callFact
        )(record)

        successWrite(Seq(request))

        val inWindow = Seq(window, window.plus(5.minute), window.plus(10.minute))
        inWindow.foreach { window =>
          successGetSomething(ForCallFactFilter(callFact.fact, window)) { r =>
            r should equal(serialize(callFact))
          }
        }
      }
    }
    "get redirect call for campaign history" when {
      behave.like(baseGetCases(RedirectCampaignCallForCampaignHistoryGen, window))
      "for only phone show event for same call" in {
        val callFactForCampaignHistory = RedirectCampaignCallForCampaignHistoryGen.next
        val callFactForPhoneShow = callFactForCampaignHistory.copy(source = EventSources.PhoneShows)
        val record = asRecord(callFactForPhoneShow)

        successWrite(Seq(record))

        val defaultFilter = ForCallFactFilter(callFactForCampaignHistory.fact, window)
        successGetSomething(defaultFilter) { r =>
          r should equal(serialize(callFactForPhoneShow))
        }
      }
    }
    "not get redirect call for campaign history" when {
      behave.like(baseNotGetCases(RedirectCampaignCallForCampaignHistoryGen, window))
    }
    "not get callback call for campaign history" when {
      "for same params" in {
        val callFact = CallbackCampaignCallForPhoneShowGen.next
        val record = asRecord(callFact)

        successWrite(Seq(record))

        successGetNothing(ForCallFactFilter(callFact.fact, window))
      }
    }
    "get callback call for campaign history" when {
      behave.like(baseGetCases(CallbackCampaignCallForCampaignHistoryGen, window))
    }
    "not get callback call for campaign history" when {
      behave.like(baseNotGetCases(CallbackCampaignCallForCampaignHistoryGen, window))
      "for only phone show event for same call" in {
        val callFactForCampaignHistory = CallbackCampaignCallForCampaignHistoryGen.next
        val callFactForPhoneShow = callFactForCampaignHistory.copy(source = EventSources.PhoneShows)
        val record = asRecord(callFactForPhoneShow)

        successWrite(Seq(record))

        val defaultFilter = ForCallFactFilter(callFactForCampaignHistory.fact, window)
        successGetNothing(defaultFilter)
      }
    }
  }
}
