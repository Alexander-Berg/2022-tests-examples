package ru.yandex.vertis.passport.dao

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.duration.DurationInt

trait PerSessionStorageSpec extends WordSpec with SpecBase {

  def perSessionStorage: PerSessionStorage

  "PerSessionStorage" should {
    "store and get data" in {
      val session = ModelGenerators.session.next
      perSessionStorage.save(session.id, "qualifier", "value", 10.seconds).futureValue
      val res = perSessionStorage.get(session.id, "qualifier").futureValue
      res.get shouldBe "value"
    }
    "return none if nothing found" in {
      val session = ModelGenerators.session.next
      val res = perSessionStorage.get(session.id, "qualifier").futureValue
      res shouldBe None
    }
  }
}
