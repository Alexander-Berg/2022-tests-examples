package ru.yandex.vertis.vsquality.hobo.dao

import org.scalacheck.Gen
import ru.yandex.vertis.vsquality.hobo.exception.{AlreadyExistException, NotExistException}
import ru.yandex.vertis.vsquality.hobo.util.SpecBase
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer

/**
  * Base specs on [[SingleValueDao]]
  *
  * @author semkagtn
  */
trait SingleValueDaoSpecBase[V] extends SpecBase {

  def valueGen: Gen[V]

  def singleValueDao: SingleValueDao[V]

  before {
    singleValueDao.delete().futureValue
  }

  "put" should {

    "correctly put value" in {
      val value = valueGen.next

      val returnedValue = singleValueDao.put(value).futureValue
      returnedValue should smartEqual(value)

      val actualValue = singleValueDao.get().futureValue
      actualValue should smartEqual(value)
    }

    "throw an exception if value already exist" in {
      val value = valueGen.next

      singleValueDao.put(value).futureValue

      whenReady(singleValueDao.put(value).failed) { e =>
        e shouldBe a[AlreadyExistException]
      }
    }
  }

  "replace" should {

    "correctly replace value" in {
      val oldValue = valueGen.next
      val newValue = valueGen.next

      singleValueDao.put(oldValue).futureValue

      val returnedValue = singleValueDao.replace(newValue).futureValue
      returnedValue should smartEqual(newValue)

      val actualValue = singleValueDao.get().futureValue
      actualValue should smartEqual(newValue)
    }

    "throw an exception if value doesn't exist" in {
      val value = valueGen.next

      whenReady(singleValueDao.replace(value).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "putOrReplace" should {

    "correctly put value" in {
      val value = valueGen.next

      val returnedValue = singleValueDao.putOrReplace(value).futureValue
      returnedValue should smartEqual(value)

      val actualValue = singleValueDao.get().futureValue
      actualValue should smartEqual(value)
    }

    "correctly replace value" in {
      val oldValue = valueGen.next
      val newValue = valueGen.next

      singleValueDao.put(oldValue).futureValue

      val returnedValue = singleValueDao.putOrReplace(newValue).futureValue
      returnedValue should smartEqual(newValue)

      val actualValue = singleValueDao.get().futureValue
      actualValue should smartEqual(newValue)
    }
  }

  "get" should {

    "throw an exception if value doesn't exist" in {
      whenReady(singleValueDao.get().failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "delete" should {

    "correctly delete value" in {
      val value = valueGen.next

      singleValueDao.put(value).futureValue
      singleValueDao.delete().futureValue

      whenReady(singleValueDao.get().failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }
}
