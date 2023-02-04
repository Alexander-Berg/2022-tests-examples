package ru.yandex.vertis.caching.base

import org.scalatest.concurrent.Eventually.{PatienceConfig, eventually}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import ru.yandex.vertis.caching.plain.base.PlainCache

import scala.concurrent.duration._

/**
  * @author korvit
  */
trait PlainCacheSpec
  extends WordSpec
    with Matchers
    with BeforeAndAfterEach {

  protected val cache: PlainCache[String, Int]

  private val simpleKey = "key"
  private val simpleValue = 1
  private val testData = Map("key1" -> 1, "key2" -> 2, "key3" -> 3)
  private val defaultExpire = 1.minute

  override def beforeEach(): Unit =
    cache.multiDelete(testData.keySet + simpleKey)

  "get" should {
    "return None for non-existing key" in {
      cache.get("key") shouldBe None
    }
  }

  "multiGet" should {
    "return empty Map for non-existing keys" in {
      cache.multiGet(testData.keySet) shouldBe Map.empty
    }
  }

  "set and get" should {
    "get return correct value after single set" in {
      cache.set(simpleKey, simpleValue, defaultExpire)
      cache.get(simpleKey) shouldBe Some(simpleValue)
    }

    "get return last value after multiply sets" in {
      cache.set(simpleKey, 1, defaultExpire)
      cache.set(simpleKey, 2, defaultExpire)
      cache.get(simpleKey) shouldBe Some(2)
    }

    "get see correct value after each of multiply sets" in {
      cache.set(simpleKey, 1, defaultExpire)
      val valueOtp1 = cache.get(simpleKey)
      cache.set(simpleKey, 2, defaultExpire)
      val valueOtp2 = cache.get(simpleKey)

      valueOtp1 shouldBe Some(1)
      valueOtp2 shouldBe Some(2)
    }
  }

  "multiSet and multiGet" should {
    "multiGet return correct values after single multiSet" in {
      cache.multiSet(testData, defaultExpire)
      cache.multiGet(testData.keySet) shouldEqual testData
    }

    "multiGet return last values after multiply multiSets" in {
      cache.multiSet(testData.mapValues(_ * 2).toMap, defaultExpire)
      cache.multiSet(testData, defaultExpire)
      cache.multiGet(testData.keySet) shouldEqual testData
    }

    "multiGet see correct values after each of multiply multiSet" in {
      val doubledTestData = testData.mapValues(_ * 2).toMap
      cache.multiSet(doubledTestData, defaultExpire)
      val cachedData1 = cache.multiGet(testData.keySet)
      cache.multiSet(testData, defaultExpire)
      val cachedData2 = cache.multiGet(testData.keySet)

      cachedData1 shouldEqual doubledTestData
      cachedData2 shouldEqual testData
    }

    "multiGet return Map with all existing key values" in {
      val nonExistingKeys = Set("key10", "key11", "key12")
      cache.multiSet(testData, defaultExpire)
      cache.multiGet(testData.keySet ++ nonExistingKeys) shouldEqual testData
    }
  }

  "get and multiGet" should {
    "agreed in terms of set" in {
      testData foreach { case (k, v) => cache.set(k, v, defaultExpire) }
      val getData = (testData.keySet map (k => k -> cache.get(k).get)).toMap
      val multiGetData = cache.multiGet(testData.keySet)
      getData shouldEqual multiGetData
    }

    "agreed in terms of multiSet" in {
      cache.multiSet(testData, defaultExpire)
      val getData = (testData.keySet map (k => k -> cache.get(k).get)).toMap
      val multiGetData = cache.multiGet(testData.keySet)
      getData shouldEqual multiGetData
    }
  }

  "set and multiSet" should {
    "agreed in terms of get" in {
      testData foreach { case (k, v) => cache.set(k, v, defaultExpire) }
      val setData = (testData.keySet map (k => k -> cache.get(k).get)).toMap

      cache.multiDelete(testData.keySet)

      cache.multiSet(testData, defaultExpire)
      val multiSetData = (testData.keySet map (k => k -> cache.get(k).get)).toMap

      setData shouldEqual multiSetData
    }

    "agreed in terms of multiGet" in {
      testData foreach { case (k, v) => cache.set(k, v, defaultExpire) }
      val setData = cache.multiGet(testData.keySet)

      cache.multiDelete(testData.keySet)

      cache.multiSet(testData, defaultExpire)
      val multiSetData = cache.multiGet(testData.keySet)

      setData shouldEqual multiSetData
    }
  }

  "expire duration" should {
    val smallExpire = 1.second

    implicit val pc = PatienceConfig(10 * smallExpire, 1.second)

    "expire with set" in {
      cache.set(simpleKey, simpleValue, smallExpire)
      Thread.sleep(smallExpire.toMillis)
      eventually {
        cache.get(simpleKey) shouldBe None
      }
    }

    "expire with multiSet" in {
      cache.multiSet(testData, smallExpire)
      Thread.sleep((5 * smallExpire).toMillis)
      eventually {
        cache.multiGet(testData.keySet) shouldBe Map.empty
      }

    }
  }

  "delete" should {
    "delete value for existing key" in {
      cache.set(simpleKey, simpleValue, defaultExpire)
      cache.delete(simpleKey)
      cache.get(simpleKey) shouldBe None
    }

    "do nothing for non-existing key" in {
      cache.delete(simpleKey)
    }
  }

  "multiDelete" should {
    "delete values for existing keys" in {
      cache.multiSet(testData, defaultExpire)
      cache.multiDelete(testData.keySet)
      cache.multiGet(testData.keySet) shouldBe Map.empty
    }

    "do nothing for non-existing keys" in {
      cache.multiDelete(testData.keySet)
    }

    "delete values for existing and do nothing for non-existing keys" in {
      val nonExistingKeys = Set("key10", "key11", "key12")

      cache.set(simpleKey, simpleValue, defaultExpire)
      cache.multiSet(testData, defaultExpire)
      cache.multiDelete(testData.keySet ++ nonExistingKeys)

      val cachedMap = cache.multiGet(testData.keySet ++ nonExistingKeys + simpleKey)
      cachedMap shouldEqual Map(simpleKey -> simpleValue)
    }
  }
}
