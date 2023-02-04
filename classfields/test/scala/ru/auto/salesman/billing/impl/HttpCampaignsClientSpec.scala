package ru.auto.salesman.billing.impl

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.auto.salesman.billing.{CampaignsClient, CampaignsClientSpec}

/** Runnable spec on [[HttpCampaignsClient]]
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class HttpCampaignsClientSpec extends CampaignsClientSpec {

  val client: CampaignsClient = new HttpCampaignsClient(CampaignsUri)
}
