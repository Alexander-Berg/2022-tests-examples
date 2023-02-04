package ru.yandex.vertis.vsquality.hobo.dao

import org.scalacheck.Gen
import ru.yandex.vertis.vsquality.hobo.util.SpecBase
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators.stringGen
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.exception.{AlreadyExistException, NotExistException}

/**
  * Base specs on [[KeyValueDao]]
  *
  * @author semkagtn
  */
trait KeyValueDaoSpecBase[V] extends SpecBase {

  def valueGen: Gen[V]

  def keyValueDao: KeyValueDao[V]

  before {
    keyValueDao.clear().futureValue
  }

  "put" should {

    "correctly put value" in {
      val key = randomKey()
      val value = valueGen.next

      val returnedValue = keyValueDao.put(key, value).futureValue
      returnedValue should smartEqual(value)

      val actualValue = keyValueDao.get(key).futureValue
      actualValue should smartEqual(value)
    }

    "throw an exception if value already exist" in {
      val key = randomKey()
      val value = valueGen.next

      keyValueDao.put(key, value).futureValue

      whenReady(keyValueDao.put(key, value).failed) { e =>
        e shouldBe a[AlreadyExistException]
      }
    }
  }

  "replace" should {

    "correctly replace value" in {
      val key = randomKey()
      val oldValue = valueGen.next
      val newValue = valueGen.next

      keyValueDao.put(key, oldValue).futureValue

      val returnedValue = keyValueDao.replace(key, newValue).futureValue
      returnedValue should smartEqual(newValue)

      val actualValue = keyValueDao.get(key).futureValue
      actualValue should smartEqual(newValue)
    }

    "throw an exception if value doesn't exist" in {
      val nonexistentKey = randomKey()
      val value = valueGen.next

      whenReady(keyValueDao.replace(nonexistentKey, value).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "putOrReplace" should {

    "correctly put value" in {
      val key = randomKey()
      val value = valueGen.next

      val returnedValue = keyValueDao.putOrReplace(key, value).futureValue
      returnedValue should smartEqual(value)

      val actualValue = keyValueDao.get(key).futureValue
      actualValue should smartEqual(value)
    }

    "correctly replace value" in {
      val key = randomKey()
      val oldValue = valueGen.next
      val newValue = valueGen.next

      keyValueDao.put(key, oldValue).futureValue

      val returnedValue = keyValueDao.putOrReplace(key, newValue).futureValue
      returnedValue should smartEqual(newValue)

      val actualValue = keyValueDao.get(key).futureValue
      actualValue should smartEqual(newValue)
    }
  }

  "get" should {

    "throw an exception if key doesn't exist" in {
      val nonexistentKey = randomKey()

      whenReady(keyValueDao.get(nonexistentKey).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "delete" should {

    "correctly delete key-value pair" in {
      val key = randomKey()
      val value = valueGen.next

      keyValueDao.put(key, value).futureValue
      keyValueDao.delete(key).futureValue

      whenReady(keyValueDao.get(key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "throw an exception if key doesn't exist" in {
      val nonexistentKey = randomKey()

      whenReady(keyValueDao.delete(nonexistentKey).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "getAll" should {

    "correctly get key-value pairs" in {
      val keys = List(randomKey(), randomKey())
      val values = valueGen.next(2).toList

      val expected =
        for ((k, v) <- keys.zip(values)) yield {
          keyValueDao.put(k, v).futureValue
          (k, v)
        }
      val actual = keyValueDao.getAll().futureValue

      actual should smartEqual(expected.toMap)
    }
  }

  "updateAll" should {

    "correctly update key-value pairs" in {
      val key1 = randomKey()
      val value1 = valueGen.next
      keyValueDao.put(key1, value1).futureValue

      val key2 = randomKey()
      val value2 = valueGen.next
      keyValueDao.put(key2, value2).futureValue

      val newValue1 = valueGen.next
      val key3 = randomKey()
      val value3 = valueGen.next
      val pairs = Map(key1 -> newValue1, key3 -> value3)
      keyValueDao.updateAll(pairs).futureValue

      val actualResult = keyValueDao.getAll().futureValue
      val expectedResult = Map(key1 -> newValue1, key2 -> value2, key3 -> value3)
      actualResult should smartEqual(expectedResult)
    }
  }

  private def randomKey(): String = stringGen(4, 6).next
}
