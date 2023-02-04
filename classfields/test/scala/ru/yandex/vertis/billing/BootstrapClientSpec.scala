package ru.yandex.vertis.billing

import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.Model.BootstrapCampaignSource

import scala.util.Failure

/**
  * Spec for [[BootstrapClient]].
  * It is hard to test bootstrap API because it requires a lot of preconditions on server-side.
  *
  * @author alex-kovalenko
  */
trait BootstrapClientSpec extends AnyWordSpec with Matchers {

  implicit val rc = RequestContext("bootstrap-client-tests")

  def getClient(domain: String): BootstrapClient

  def source: BootstrapCampaignSource

  "BootstrapClient" should {
    "fail with UnsupportedOperationException for domain without Bootstrap API" in {
      val c = getClient("autoru")
      c.campaign(source) match {
        case Failure(_: UnsupportedOperationException) =>
        case other => fail(s"Unexpected $other")
      }
    }

    "create new campaign" in {
      val c = getClient("autoru")
      val response = c.campaign(source).get
      // some checks here, or just look into DB and logs
    }
  }
}
