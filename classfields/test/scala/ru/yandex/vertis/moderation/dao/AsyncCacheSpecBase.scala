package ru.yandex.vertis.moderation.dao

import org.scalacheck.Gen
import ru.yandex.vertis.caching.base.AsyncCache
import ru.yandex.vertis.caching.base.layout.Layout
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.impl.layout.Layouts
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{ExternalId, Instance}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author semkagtn
  */
trait AsyncCacheSpecBase extends SpecBase {

  type Key = ExternalId
  type Value = Instance

  final val KeyGen: Gen[Key] = CoreGenerators.ExternalIdGen
  final val ValueGen: Gen[Value] = CoreGenerators.InstanceGen

  final protected val layout: Layout[Key, Value] = Layouts.UserUpdatesLayout

  def asyncCache: AsyncCache[Key, Value]

  "get" should {

    "return existent value" in {
      val key = KeyGen.next
      val value = ValueGen.next
      asyncCache.set(key, value, Duration.Inf).futureValue

      val actualResult = asyncCache.get(key).futureValue
      val expectedResult = Some(value)
      actualResult shouldBe expectedResult
    }

    "return None if non-existent key" in {
      val key = KeyGen.next

      val actualResult = asyncCache.get(key).futureValue
      val expectedResult = None
      actualResult shouldBe expectedResult
    }
  }

  "set" should {

    "correctly creates value with finite expire" in {
      val key = KeyGen.next
      val value = ValueGen.next
      val expire = 1.second
      asyncCache.set(key, value, expire).futureValue

      Thread.sleep((expire + 1.second).toMillis)
      val actualResult = asyncCache.get(key).futureValue
      val expectedResult = None
      actualResult shouldBe expectedResult
    }

    "correctly updates existent value" in {
      val key = KeyGen.next
      val value1 = ValueGen.next
      val value2 = ValueGen.next

      asyncCache.set(key, value1, Duration.Inf).futureValue
      asyncCache.set(key, value2, Duration.Inf).futureValue

      val actualResult = asyncCache.get(key).futureValue
      val expectedResult = Some(value2)
      actualResult shouldBe expectedResult
    }
  }

  "delete" should {

    "correctly deletes value" in {
      val key = KeyGen.next
      val value = ValueGen.next
      asyncCache.set(key, value, Duration.Inf).futureValue
      asyncCache.delete(key).futureValue

      val actualResult = asyncCache.get(key).futureValue
      val expectedResult = None
      actualResult shouldBe expectedResult
    }

    "do nothing if non-existent key" in {
      val key = KeyGen.next
      asyncCache.delete(key).futureValue
    }
  }
}
