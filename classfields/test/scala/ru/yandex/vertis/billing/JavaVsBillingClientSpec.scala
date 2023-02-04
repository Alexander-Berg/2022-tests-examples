package ru.yandex.vertis.billing

import java.io.IOException
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.vertis.billing.gens.CampaignsClientMock

/**
  * Spec for [[JavaCampaignsClient]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class JavaVsBillingClientSpec extends FlatSpec with Matchers {

  val client = new JavaCampaignsClient(CampaignsClientMock)

  "JavaBillingClient" should "get campaigns" in {
    assert(client.getCampaignHeaders.size > 0)
    assert(client.getCampaignHeaders(true).size() > 0)

    assert(client.getValuableCampaigns(true).size > 0)
    assert(client.getValuableCampaigns(false).size > 0)

    assert(client.getCampaignHeader("1") != null)
    assert(client.getCampaignHeader("2") == null)

    assert(client.getCampaignHeader("1", thin = false) != null)
    assert(client.getCampaignHeader("2", thin = true) == null)
    intercept[IOException] {
      client.getCampaignHeader("3")
    }
    intercept[IllegalArgumentException] {
      client.getCampaignHeader("4")
    }
  }
}
