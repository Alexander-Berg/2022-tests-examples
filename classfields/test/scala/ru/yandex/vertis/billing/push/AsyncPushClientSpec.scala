package ru.yandex.vertis.billing.push

import java.util.concurrent.Executors

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.{OfferBillingGen, Producer}
import ru.yandex.vertis.billing.model_core.{OfferId, PartnerOfferId}

import scala.concurrent.{Await, ExecutionContext}

/**
  * Specs on [[AsyncPushClient]]
  *
  * @author ruslansd
  */
trait AsyncPushClientSpec extends AnyWordSpec with Matchers {

  def client: AsyncPushClient

  import scala.concurrent.duration._

  val threadPool = Executors.newFixedThreadPool(1)

  val timeout = 10.second

  implicit val ec = ExecutionContext.fromExecutorService(threadPool)

  val InvalidTarget = PartnerOfferId("0", "1")
  val partnerId = "1438536"
  val ExistTarget = PartnerOfferId(partnerId, "7639799169584022780")

  val ExistTargets = Set[OfferId](
    ExistTarget,
    PartnerOfferId(partnerId, "9105113956148520470"),
    PartnerOfferId(partnerId, "3276597006312821983")
  )

  "PushClient" should {
    "get no info for invalid target" in {
      intercept[NoSuchElementException] {
        Await.result(client.get(InvalidTarget), timeout)

      }
    }
    "set, get and skip billing data for exists target" in {
      val billing = OfferBillingGen.next
      Await.result(client.set(ExistTarget, billing), timeout)
      Await.result(client.get(ExistTarget), 10.second) should be(billing)

      Await.result(client.skip(ExistTarget), timeout)
      intercept[NoSuchElementException] {
        Await.result(client.get(ExistTarget), timeout)
      }
    }
  }
}
