package ru.yandex.vertis.caching.support

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import ru.yandex.vertis.caching.plain.base.PlainCache
import ru.yandex.vertis.caching.plain.support.PlainCacheSupport
import ru.yandex.vertis.caching.support.CacheControl.{Cache, NoCache}

import scala.concurrent.duration._
import language.reflectiveCalls

/**
  * @author korvit
  */
trait PlainCacheSupportSpec
  extends WordSpec
    with Matchers
    with BeforeAndAfterEach {

  protected def newPlainCache(): PlainCache[String, Int]

  protected def newPlainCacheSupport[K,V](plainCache: PlainCache[K, V]): PlainCacheSupport[String, Int]

  def fixture = new {
    val cache: PlainCache[String, Int] = newPlainCache()
    val cacheSupport: PlainCacheSupport[String, Int] = newPlainCacheSupport(cache)
  }

  private val simpleKey = "0"
  private val simpleValue = 0
  private val testData = Map("1" -> 1, "2" -> 2, "3" -> 3)
  private val defaultExpire = 1.minute


  private val loader = (key: String) => key.toInt
  private lazy val failedLoader = throw new IllegalArgumentException
  private val multiSuccessLoader = (keys: Set[String]) =>
      (keys map (key => key -> loader(key))).toMap
  private val multiFailedLoader = (_: Set[String]) =>
    failedLoader

  private def multiMixLoader(successKeys: Set[String]) =
    (keys: Set[String]) =>
        keys.map { key =>
          key -> (if (successKeys.contains(key)) loader(key) else failedLoader)
        }.toMap

  "single `withCacheControl`" should {
    "return exiting value from cache with `CacheControl.Cache`" in {
      val f = fixture
      f.cache.set(simpleKey, simpleValue, defaultExpire)
      val cached = f.cacheSupport.withCacheControl(simpleKey, defaultExpire, Cache)(failedLoader)
      cached shouldBe simpleValue
    }

    "calculate and set non-exiting value with `CacheControl.Cache`" in {
      val f = fixture
      val calculated = f.cacheSupport.withCacheControl(simpleKey, defaultExpire, Cache)(loader(simpleKey))
      val cachedOpt = f.cache.get(simpleKey)
      calculated shouldBe simpleValue
      cachedOpt shouldBe Some(simpleValue)
    }

    "calculate existing value and update cache with `CacheControl.NoCache`" in {
      val f = fixture
      f.cache.set(simpleKey, 0, defaultExpire)
      val calculated = f.cacheSupport.withCacheControl(simpleKey, defaultExpire, NoCache)(loader(simpleKey))
      val cachedOpt = f.cache.get(simpleKey)
      calculated shouldBe simpleValue
      cachedOpt shouldBe Some(simpleValue)
    }

    "calculate non-existing value and set into cache with `CacheControl.NoCache`" in {
      val f = fixture
      val calculated = f.cacheSupport.withCacheControl(simpleKey, defaultExpire, NoCache)(loader(simpleKey))
      val cachedOpt = f.cache.get(simpleKey)
      calculated shouldBe simpleValue
      cachedOpt shouldBe Some(simpleValue)
    }
  }

  "multi `withCacheControl`" should {
    "return exiting values from cache with `CacheControl.Cache`" in {
      val f = fixture
      f.cache.multiSet(testData, defaultExpire)
      val cached = f.cacheSupport.withCacheControl(testData.keySet, defaultExpire, Cache)(multiFailedLoader)
      cached shouldBe testData
    }

    "calculate and set non-exiting values with `CacheControl.Cache`" in {
      val f = fixture
      val calculated = f.cacheSupport.withCacheControl(testData.keySet, defaultExpire, Cache)(multiSuccessLoader)
      val cached = f.cache.multiGet(testData.keySet)
      calculated shouldBe testData
      cached shouldBe testData
    }

    "return exiting values from cache, calculate and set non-exiting with `CacheControl.Cache`" in {
      val f = fixture
      val existing = testData
      val nonExiting = existing.map {
        case (k, v) => (k.toInt * 10).toString -> (v * 10)
      }
      f.cache.multiSet(existing, defaultExpire)
      val full = f.cacheSupport.withCacheControl(
        existing.keySet ++ nonExiting.keySet,
        defaultExpire,
        Cache)(multiMixLoader(nonExiting.keySet))
      val cached = f.cache.multiGet(existing.keySet ++ nonExiting.keySet)
      full shouldBe existing ++ nonExiting
      cached shouldBe existing ++ nonExiting
    }

    "calculate exiting values and update cache with `CacheControl.NoCache`" in {
      val f = fixture
      f.cache.multiSet(testData.mapValues(_ * 2).toMap, defaultExpire)
      val calculated = f.cacheSupport.withCacheControl(testData.keySet, defaultExpire, NoCache)(multiSuccessLoader)
      val cached = f.cache.multiGet(testData.keySet)
      calculated shouldBe testData
      cached shouldBe testData
    }

    "calculate non-exiting values and set into cache with `CacheControl.NoCache`" in {
      val f = fixture
      val calculated = f.cacheSupport.withCacheControl(testData.keySet, defaultExpire, NoCache)(multiSuccessLoader)
      val cached = f.cache.multiGet(testData.keySet)
      calculated shouldBe testData
      cached shouldBe testData
    }

    "calculate and update in cache both existing and non-exiting values with `CacheControl.NoCache`" in {
      val f = fixture
      val oldExisting = testData.mapValues(_ * 2).toMap
      val newExisting = testData
      val nonExiting = testData.map {
        case (k, v) => (k.toInt * 10).toString -> (v * 10)
      }
      f.cache.multiSet(oldExisting, defaultExpire)
      val full = f.cacheSupport.withCacheControl(
        oldExisting.keySet ++ nonExiting.keySet,
        defaultExpire,
        NoCache)(multiSuccessLoader)
      val cached = f.cache.multiGet(oldExisting.keySet ++ nonExiting.keySet)
      full shouldBe newExisting ++ nonExiting
      cached shouldBe newExisting ++ nonExiting
    }
  }
}

