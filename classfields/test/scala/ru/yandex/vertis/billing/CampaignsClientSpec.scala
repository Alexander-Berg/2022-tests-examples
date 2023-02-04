package ru.yandex.vertis.billing

import CampaignsClientSpec._
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.CampaignsClient.Options

import scala.util.Success

/**
  * Base specs for [[ru.yandex.vertis.billing.CampaignsClient]]
  *
  * @author alesavin
  */
trait CampaignsClientSpec extends AnyWordSpec with Matchers {

  def client: CampaignsClient

  "VsBillingClient" should {
    "not get campaign header by unknown ID" in {
      client.getCampaignHeader("non-exist") should be(Success(None))
    }

    "get campaign header with long ID" in {
      client.getCampaignHeader(ExistLongCampaign) match {
        case Success(Some(ch)) => ch.getId should be(ExistLongCampaign)
        case other => fail(s"Unexpected $other")
      }
    }

    "get campaign header with string ID" in {
      client.getCampaignHeader(ExistStringCampaign) match {
        case Success(Some(ch)) =>
          ch.getId should be(ExistStringCampaign)
        case other => fail(s"Unexpected $other")
      }
    }

    "get all campaigns" in {
      client.getCampaignHeaders match {
        case Success(iterable) => iterable.size should be > 0
        case other => fail(s"Unpredicted $other")
      }
    }

    "get all campaigns with essential=false" in {
      client.getCampaignHeaders(Options(Some(false))) match {
        case Success(iterable) => iterable.size should be > 0
        case other => fail(s"Unpredicted $other")
      }
    }

    "get all campaigns essentials" in {
      client.getCampaignHeaders(Options(Some(true))) match {
        case Success(iterable) => iterable.size should be > 0
        case other => fail(s"Unpredicted $other")
      }
    }

    "get campaign essential by ID" in {
      client.getCampaignHeader(ExistStringCampaign, Options(Some(true))) match {
        case Success(Some(ch)) =>
          ch.getId should be(ExistStringCampaign)
        case other => fail(s"Unpredicted $other")
      }
    }

    "get valuable campaigns" in {
      client.getValuableCampaigns() match {
        case Success(iterable) => iterable.size should be > 0
        case other => fail(s"Unpredicted $other")
      }
    }
  }
}

object CampaignsClientSpec {
  val ExistLongCampaign = "379" // not exists
  val ExistStringCampaign = "00eb999c-8436-40ff-a366-e41d6e84b01a" // exists in dev-store02f/vs_billing_realty
}
