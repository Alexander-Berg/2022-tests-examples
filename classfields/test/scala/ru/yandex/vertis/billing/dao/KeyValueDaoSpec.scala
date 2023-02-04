package ru.yandex.vertis.billing.dao

import ru.yandex.vertis.billing.async.AsyncSpecBase
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Success

/**
  * Specs on [[KeyValueDao]]
  *
  * @author dimas
  */
trait KeyValueDaoSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  def keyValueDao: KeyValueDao

  "KeyValueDao" should {
    "get all key-value pairs" in {
      keyValueDao.putT("k1", "v1").get
      keyValueDao.putT("k2", "v2").get
      keyValueDao.getAll match {
        case Success(map) =>
          map should (have size 2 and contain("k1" -> "v1") and contain("k2" -> "v2"))
        case other =>
          fail(s"unexpected result: $other")
      }
    }

    "get all key-value pairs with Future function" in {
      keyValueDao.put("k1", "v1").futureValue
      keyValueDao.put("k2", "v2").futureValue
      keyValueDao.getAll match {
        case Success(map) =>
          map should (have size 2 and contain("k1" -> "v1") and contain("k2" -> "v2"))
        case other =>
          fail(s"unexpected result: $other")
      }
    }

    "put and get value by key" in {
      keyValueDao.putT("foo", "bar").get
      keyValueDao.getT("foo") should be(Success("bar"))
    }

    "put and get value by key with Future function" in {
      keyValueDao.put("foo", "bar").futureValue
      keyValueDao.get("foo").futureValue should be("bar")
    }

    "not get value by absent key" in {
      intercept[NoSuchElementException] {
        keyValueDao.getT("baz").get
      }
    }

    "not get value by absent key with Future function" in {
      keyValueDao.get("baz").failed.futureValue shouldBe an[NoSuchElementException]
    }

    "delete keys" in {
      val key = "test1"
      val value = "test1"
      (keyValueDao.putT(key, value) should be).a(Symbol("Success"))
      keyValueDao.getT(key) shouldBe Success(value)
      (keyValueDao.delete(key) should be).a(Symbol("Success"))

      intercept[NoSuchElementException] {
        keyValueDao.getT(key).get
      }
    }
  }

}
