package ru.yandex.auto.vin.decoder.hydra

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import auto.carfax.common.utils.concurrent.CoreFutureUtils._

import scala.concurrent.ExecutionContext.Implicits.global

class AvilonRequestsClickerSpec extends AnyWordSpecLike with Matchers {

  implicit val t = Traced.empty

  "AvilonRequestsClicker" should {

    "return correct available clicks" in {
      val client = new HydraClientStub(10)
      val clicker = new AvilonRequestsClicker(client, 250, 10)
      val remClicks = clicker.getAvaliableClicks.await

      remClicks shouldBe 20
    }

    "correct countdown available clicks" in {
      val client = new HydraClientStub(10)
      val clicker = new AvilonRequestsClicker(client, 10, 9)
      val remClicks1 = clicker.getAvaliableClicks.await

      remClicks1 shouldBe 1

      clicker.inc.await

      val remClicks2 = clicker.getAvaliableClicks.await

      remClicks2 shouldBe 0
    }

    "do not return negative clicks" in {
      val client = new HydraClientStub(10)
      val clicker = new AvilonRequestsClicker(client, 10, 9)
      clicker.inc
      clicker.inc

      val res = clicker.getAvaliableClicks.await

      res should not be -1
      res shouldBe 0
    }
  }
}
