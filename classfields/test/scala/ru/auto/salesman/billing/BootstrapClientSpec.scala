package ru.auto.salesman.billing

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.billing.Model.BootstrapCampaignSource

import scala.util.Failure

/** Spec for [[BootstrapClient]].
  * It is hard to test bootstrap API because it requires a lot of preconditions on server-side.
  *
  * @author alex-kovalenko
  */
trait BootstrapClientSpec extends Matchers with WordSpecLike {

  implicit val rc: RequestContext = RequestContext("bootstrap-client-tests")

  def getClient(domain: String): BootstrapClient

  def source: BootstrapCampaignSource

  "BootstrapClient" should {
    "fail with UnsupportedOperationException for domain without Bootstrap API" in {
      val c = getClient("rabota")
      c.campaign(source) match {
        case Failure(_: UnsupportedOperationException) =>
        case other => fail(s"Unexpected $other")
      }
    }

    "create new campaign" in {
      val c = getClient("autoru")
      c.campaign(source).get
      // some checks here, or just look into DB and logs
    }
  }
}
