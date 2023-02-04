package ru.yandex.vertis.billing.dao

import ru.yandex.vertis.billing.dao.CallsSearchDao.ForCallFactFilter
import ru.yandex.vertis.billing.model_core.BaggagePayload.EventSources
import ru.yandex.vertis.billing.model_core.gens.{
  campaignCallGen,
  CampaignCallGenParams,
  Producer,
  TeleponyCallFactGenCallTypes
}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
  * Spec on [[CallsSearchDao]]
  *
  * @author ruslansd
  */
trait CallsSearchDao2Spec extends CallsSearchAutocloseableDaoBaseSpec {

  private val window: FiniteDuration = 4.day

  "CallSearchDao" should {
    "correctly work with redirect calls" when {
      "for phone show events" in {
        val facts = RedirectCampaignCallForPhoneShowGen.next(1000)

        val records = facts
          .map(asRecord)
          .map(shiftTime())

        successWrite(records)

        facts.foreach { f =>
          successGetSomething(ForCallFactFilter(f.fact, window)) { value =>
            value should equal(serialize(f))
          }
        }
      }
      "for different event sources" in {
        val facts = campaignCallGen(
          CampaignCallGenParams().withCallType(TeleponyCallFactGenCallTypes.Redirect)
        ).next(1000)
        val records = facts.map(asRecord).map(shiftTime())

        successWrite(records)

        facts.foreach { f =>
          successGetSomething(ForCallFactFilter(f.fact, window)) { value =>
            value should equal(serialize(f))
          }
        }
      }
      "extract only phone show when got campaign history too" in {
        val facts = campaignCallGen(
          CampaignCallGenParams().withCallType(TeleponyCallFactGenCallTypes.Redirect)
        ).next(1000)

        val byPhoneShow = facts
          .map(_.copy(source = EventSources.PhoneShows))
        val byCampaignEvent = facts
          .map(_.copy(source = EventSources.CampaignHistory))
        val records =
          byPhoneShow.map(asRecord).map(shiftTime(10.minutes)) ++
            byCampaignEvent.map(asRecord).map(shiftTime(5.minutes))

        successWrite(records)

        facts.foreach { f =>
          successGetSomething(ForCallFactFilter(f.fact, window)) { value =>
            value should equal(serialize(f.copy(source = EventSources.PhoneShows)))
          }
        }
      }
    }
    "correctly work with callback calls" should {
      "for campaign history events" in {
        val facts = RedirectCampaignCallForCampaignHistoryGen.next(1000)

        val records = facts
          .map(asRecord)
          .map(shiftTime())

        successWrite(records)

        facts.foreach { f =>
          successGetSomething(ForCallFactFilter(f.fact, window)) { value =>
            value should equal(serialize(f))
          }
        }
      }
      "for different event sources" in {
        val facts = campaignCallGen(
          CampaignCallGenParams().withCallType(TeleponyCallFactGenCallTypes.Callback)
        ).next(1000)
        val records = facts.map(asRecord).map(shiftTime())

        successWrite(records)

        facts.foreach {
          case f if f.source == EventSources.PhoneShows =>
            successGetNothing(ForCallFactFilter(f.fact, window))
          case f if f.source == EventSources.CampaignHistory =>
            successGetSomething(ForCallFactFilter(f.fact, window)) { value =>
              value should equal(serialize(f))
            }
          case _ => sys.error("Impossible")
        }
      }
      "extract only campaign history when got phone show too" in {
        val facts = campaignCallGen(
          CampaignCallGenParams().withCallType(TeleponyCallFactGenCallTypes.Callback)
        ).next(1000)

        val byPhoneShow = facts
          .map(_.copy(source = EventSources.PhoneShows))
        val byCampaignEvent = facts
          .map(_.copy(source = EventSources.CampaignHistory))
        val records =
          byPhoneShow.map(asRecord).map(shiftTime(10.minutes)) ++
            byCampaignEvent.map(asRecord).map(shiftTime(5.minutes))

        successWrite(records)

        facts.foreach { f =>
          successGetSomething(ForCallFactFilter(f.fact, window)) { value =>
            value should equal(serialize(f.copy(source = EventSources.CampaignHistory)))
          }
        }
      }
    }
  }
}
