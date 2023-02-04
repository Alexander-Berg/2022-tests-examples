package ru.yandex.vertis.passport.dao

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

/**
  *
  * @author zvez
  */
trait KeyValueDaoSpec extends WordSpec with SpecBase {

  val dao: KeyValueDao

  "KeyValueDao" should {

    "return None if key not found" in {
      dao.get("42").futureValue shouldBe None
    }

    "create value" in {
      val key = ModelGenerators.readableString.next
      val value = ModelGenerators.readableString.next
      dao.set(key, value).futureValue

      dao.get(key).futureValue shouldBe Some(value)
    }

    "update value" in {
      val key = ModelGenerators.readableString.next
      val value = ModelGenerators.readableString.next
      dao.set(key, value).futureValue
      dao.get(key).futureValue shouldBe Some(value)

      val value2 = ModelGenerators.readableString.next
      dao.set(key, value2).futureValue
      dao.get(key).futureValue shouldBe Some(value2)
    }

  }

}
