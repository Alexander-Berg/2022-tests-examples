package ru.yandex.vertis.billing.impl

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.billing.{CampaignsClient, CampaignsClientSpec}

/**
  * Runnable spec on [[HttpCampaignsClient]]
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class HttpCampaignsClientSpec extends CampaignsClientSpec {

  val client: CampaignsClient = new HttpCampaignsClient(CampaignsUri)
}
