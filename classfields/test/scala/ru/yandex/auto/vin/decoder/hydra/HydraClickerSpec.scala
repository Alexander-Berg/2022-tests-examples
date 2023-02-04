package ru.yandex.auto.vin.decoder.hydra

import auto.carfax.common.clients.hydra.{HydraClicker, HydraClient}
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import auto.carfax.common.utils.concurrent.CoreFutureUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HydraClickerSpec extends AnyWordSpecLike with Matchers {

  implicit val t = Traced.empty

  "HydraClicker" should {

    "increment clicker then executing limited action" in {
      val client = new HydraClientStub(10)
      val clicker = buildHydraClicker(client)
      clicker.limitedFutureAction(Future.unit).await
      val curClicks = client.getClicker("", "").await

      curClicks shouldBe 1
    }

    "do not increment clicker then executing limited action without inc" in {
      val client = new HydraClientStub(10)
      val clicker = buildHydraClicker(client)
      clicker.limitedFutureActionWithoutInc(Future.unit).await
      val curClicks = client.getClicker("", "").await

      curClicks shouldBe 0
    }

    "increment clicker then inc function" in {
      val client = new HydraClientStub(10)
      val clicker = buildHydraClicker(client)
      clicker.inc.await
      val curClicks = client.getClicker("", "").await

      curClicks shouldBe 1
    }
  }

  private def buildHydraClicker(client: HydraClient): HydraClicker = new HydraClicker(client) {
    override val component: String = ""
    override val id: String = ""
    override val maxClicks: Int = 10
  }

}
