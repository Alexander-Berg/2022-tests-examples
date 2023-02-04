package ru.auto.salesman.billing.impl

import java.io.IOException

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Ignore, Matchers}
import ru.auto.salesman.billing.CampaignsClient.Options
import ru.auto.salesman.billing.gens.CampaignsClientMock

import scala.util.{Failure, Success}

/** Spec for [[CachedCampaignsClient]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class CachedVsBillingClientSpec extends FlatSpec with Matchers {

  val client = new CachedCampaignsClient(
    CampaignsClientMock,
    cacheInMinutes = 1,
    maxSize = 4
  )

  "CachedVsBillingClient" should "get campaigns" in {
    client.getCampaignHeader("1") match {
      case Success(Some(_)) => ()
      case other => fail(s"Unpredicted $other")
    }
    client.getCampaignHeader("2") match {
      case Success(None) => ()
      case other => fail(s"Unpredicted $other")
    }
    client.getCampaignHeader("3") match {
      case Failure(e: IOException) => ()
      case other => fail(s"Unpredicted $other")
    }
    client.getCampaignHeader("4") match {
      case Failure(e: IllegalArgumentException) => ()
      case other => fail(s"Unpredicted $other")
    }

    client.getCampaignHeader("1", Options(Some(true))) match {
      case Success(Some(_)) => ()
      case other => fail(s"Unpredicted $other")
    }
    client.getCampaignHeader("2", Options(Some(true))) match {
      case Success(None) => ()
      case other => fail(s"Unpredicted $other")
    }

    client.cache.asMap().size() should be(4)
    assert(Option(client.cache.getIfPresent("1")).isDefined)
    assert(Option(client.cache.getIfPresent("1")).get.isDefined)
    assert(Option(client.cache.getIfPresent("2")).isDefined)
    assert(Option(client.cache.getIfPresent("2")).get.isEmpty)
    assert(Option(client.cache.getIfPresent("3")).isEmpty)
    assert(Option(client.cache.getIfPresent("4")).isEmpty)
    assert(Option(client.cache.getIfPresent("1thin=true")).isDefined)
    assert(Option(client.cache.getIfPresent("1thin=true")).get.isDefined)
    assert(Option(client.cache.getIfPresent("2thin=true")).isDefined)
    assert(Option(client.cache.getIfPresent("2thin=true")).get.isEmpty)
  }
}
