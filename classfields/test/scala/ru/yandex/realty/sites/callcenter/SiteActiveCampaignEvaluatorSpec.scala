package ru.yandex.realty.sites.callcenter

import org.joda.time.Instant
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.sites.campaign.CampaignStorage

import java.time.Duration
import java.util
import java.util.Collections

@RunWith(classOf[JUnitRunner])
class SiteActiveCampaignEvaluatorSpec extends SpecBase {

  private val site = new Site(1L)

  "SiteActiveCampaignEvaluator" should {
    "answer that site has active campaigns" in {
      val campaignStorage = new CampaignStorage(
        java.util.List.of(
          prepareCampaign("3", active = false, Some(Instant.ofEpochSecond(1222121L))),
          prepareCampaign("6", active = true, None)
        )
      )
      val evaluator = new SiteActiveCampaignEvaluator {
        val campaignProvider: Provider[CampaignStorage] = () => campaignStorage
        val allowableCampaignInactivePeriod: Duration = Duration.parse("PT172800S")
      }

      val hasActiveCampaigns = evaluator.hasActiveCampaigns(site)

      hasActiveCampaigns shouldBe true
    }
    "answer that site has active campaigns if campaign is inactive no more than two days" in {
      val almostTwoDaysAgo = Instant.now().minus(org.joda.time.Duration.standardHours(47))
      val campaignStorage = new CampaignStorage(
        java.util.List.of(
          prepareCampaign("7", active = false, Some(almostTwoDaysAgo)),
          prepareCampaign("43", active = false, Some(Instant.ofEpochSecond(234234324L)))
        )
      )
      val evaluator = new SiteActiveCampaignEvaluator {
        val campaignProvider: Provider[CampaignStorage] = () => campaignStorage
        val allowableCampaignInactivePeriod: Duration = Duration.parse("PT172800S")
      }

      val hasActiveCampaigns = evaluator.hasActiveCampaigns(site)

      hasActiveCampaigns shouldBe true
    }
    "answer that site has no active campaigns" in {
      val campaignStorage = new CampaignStorage(
        java.util.List.of(
          prepareCampaign("1", active = false, Some(Instant.ofEpochSecond(1222121L))),
          prepareCampaign("2", active = false, Some(Instant.ofEpochSecond(234234324L)))
        )
      )
      val evaluator = new SiteActiveCampaignEvaluator {
        val campaignProvider: Provider[CampaignStorage] = () => campaignStorage
        val allowableCampaignInactivePeriod: Duration = Duration.parse("PT172800S")
      }

      val hasActiveCampaigns = evaluator.hasActiveCampaigns(site)

      hasActiveCampaigns shouldBe false
    }
    "answer that site has no active campaigns if no site campaigns exists" in {
      val campaignStorage = new CampaignStorage(Collections.emptyList())
      val evaluator = new SiteActiveCampaignEvaluator {
        val campaignProvider: Provider[CampaignStorage] = () => campaignStorage
        val allowableCampaignInactivePeriod: Duration = Duration.parse("PT172800S")
      }

      val hasActiveCampaigns = evaluator.hasActiveCampaigns(site)

      hasActiveCampaigns shouldBe false
    }
  }

  private def prepareCampaign(id: String, active: Boolean, inactiveSince: Option[Instant]) = {
    new Campaign(
      id,
      1L,
      0,
      "0",
      new util.HashMap(),
      Collections.emptyList(),
      Collections.emptyList(),
      0,
      0,
      active,
      false,
      0,
      0L,
      null,
      new util.HashMap(),
      null,
      inactiveSince.orNull
    )
  }
}
