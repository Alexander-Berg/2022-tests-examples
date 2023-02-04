package ru.yandex.vertis.passport.dao

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

import scala.concurrent.duration.DurationDouble

/**
  *
  * @author zvez
  */
trait MarkerDaoSpec extends WordSpec with SpecBase {

  def dao: MarkerDao

  val ttl = 5.minutes

  "MarkerDao" should {
    "store mark" in {
      val key = ModelGenerators.readableString.next
      dao.hasMark(key).futureValue shouldBe false
      dao.mark(key, ttl).futureValue
      dao.hasMark(key).futureValue shouldBe true
    }
  }

}
