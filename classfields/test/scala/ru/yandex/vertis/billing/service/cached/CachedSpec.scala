package ru.yandex.vertis.billing.service.cached

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.model_core.gens.{OfferBillingGen, Producer}
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.service.cached.CachedSpec._
import ru.yandex.vertis.billing.service.cached.impl.{CompositeCodec, IterableProtoCodec, KryoCodec, LoggingCache}
import ru.yandex.vertis.billing.util.CacheControl.Cache

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

/**
  * @author @logab
  */
class CachedSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  trait Env {

    val Underlying =
      new NeverExpireCache() with LoggingCache

    val Cached = new Cached {

      override def support: Cache = Underlying
      override def serviceNamespace: String = "test"
    }

    def toKey = Cached.usingServiceNamespace _

    def load(key: Key) = Try(Value(key))

    def exist(key: Key) =
      Underlying.get(toKey(key.toString)) should matchPattern { case Success(Some(_)) =>
      }

    def check(key: Key, v: Option[Value] = None) = {
      val value = v.getOrElse(Value(key))
      Underlying.get(toKey(key.toString)) should
        be(Success(Some(value)))
    }
  }

  trait ProtoEnv extends Env {

    override val Underlying =
      new NeverExpireCache(IterableProtoCodec(Model.OfferBilling.parser())) with LoggingCache
  }

  trait CompositeEnv extends Env {
    val iterableProtoCodec = IterableProtoCodec(Model.OfferBilling.parser())
    val codec = new CompositeCodec(Seq(iterableProtoCodec, KryoCodec))

    override val Underlying =
      new NeverExpireCache(codec) with LoggingCache
  }

  "Cached" should {
    "cache single value" in {
      new Env {
        val key = Key(1)
        Cached.withCache[Key, Value](key, Expire)(_.toString)(load(key))
        exist(key)
        check(key)
      }
    }
    "use cache value" in {
      new Env {
        val key = Key(1)
        Cached.withCache[Key, Value](key, Expire)(_.toString)(load(key))
        Cached.withCache[Key, Value](key, Expire)(_.toString)(load(Key(2)))
        exist(key)
        check(key)
      }
    }
    "refresh cache value" in {
      new Env {
        val key = Key(1)
        Cached.withCache[Key, Value](key, Expire)(_.toString)(load(key))
        Cached.withCacheRefresh[Key, Value](key, Expire)(_.toString)(load(Key(2)))
        exist(key)
        check(key, Some(Value(Key(2))))
      }
    }
    "cache multiple values" in {
      new Env {
        val keys = (1L to 1000L).map(Key)
        Cached.withCache[Key, Value](keys, Expire)(_.toString) { keys =>
          Try {
            keys.map(key => key -> Value(key)).toMap
          }
        }
        keys.foreach {
          check(_)
        }
      }
    }
    "add only not cached values" in {
      new Env {
        val keys = (1L to 10L).map(Key)
        Cached.withCache(keys.head, Expire)(_.toString) {
          Try {
            Value(keys.head)
          }
        }
        val results =
          Cached.withCache[Key, Value](keys, Expire)(_.toString) { elements =>
            elements should have size keys.size - 1
            Try {
              elements.map(key => key -> Value(key)).toMap
            }
          }
        results.get should be(keys.map(key => key -> Value(key)).toMap)
      }
    }
    "cache value by future" in {
      implicit val executor =
        scala.concurrent.ExecutionContext.global

      new Env {
        val key = Key(1)
        val f = Cached.withCacheAsync[Key, Value](key, Expire, Cache)(_.toString) {
          Future.successful(load(key).get)
        }
        Await.result(f, 1.second)
        exist(key)
        check(key)
      }
    }
    "fail if cache non-proto value by iterable proto codec" in {
      new ProtoEnv {
        val key = Key(1)
        val effectiveKey = toKey(key.toString)
        val value = Iterable(load(key).get)
        Underlying.set[Iterable[Value]](effectiveKey, value, Expire) match {
          case Failure(e: IllegalArgumentException) =>
            info(s"Done ${e.getMessage}")
          case other => fail(s"Unexpected $other")
        }
      }
    }
    "cache value by iterable proto codec" in {
      new ProtoEnv {
        val key = Key(1)
        val effectiveKey = toKey(key.toString)
        val value = OfferBillingGen.next(1).map(Conversions.toMessage).toList
        Underlying.set[Iterable[Model.OfferBilling]](effectiveKey, value, Expire)
      }
    }
    "cache refresh for multiple values" in {
      new Env {
        val keys = (1L to 50L).map(Key)
        def shiftKey(key: Key) = Key(20000 + key.id)

        Cached.withCache[Key, Value](keys, Expire)(_.toString) { keys =>
          Try {
            keys.map(key => key -> Value(key)).toMap
          }
        }
        keys.foreach {
          check(_)
        }

        Cached.withCacheRefresh[Key, Value](keys, Expire)(_.toString) { keys =>
          Try {
            keys.map(key => key -> Value(shiftKey(key))).toMap
          }
        }
        keys.foreach { k =>
          check(k, Some(Value(shiftKey(k))))
        }

        Cached.withCache[Key, Value](keys, Expire)(_.toString) { keys =>
          Try {
            keys.map(key => key -> Value(key)).toMap
          }
        }
        keys.foreach { k =>
          check(k, Some(Value(shiftKey(k))))
        }
      }
    }

    "cache with composite codec" in {
      new CompositeEnv {

        val key = Key(1)
        val effectiveKey = toKey(key.toString)
        val value = OfferBillingGen.next(1).map(Conversions.toMessage).toList
        Underlying.asyncSet[Iterable[Model.OfferBilling]](effectiveKey, value, Expire).toTry shouldBe Success(())

        val bytesValue = Underlying.get[Iterable[Model.OfferBilling]](effectiveKey).get.get
        bytesValue shouldEqual value

        val otherValue = 42
        Underlying.asyncSet(effectiveKey, otherValue, Expire).toTry shouldBe Success(())
        val bytesValue2 = Underlying.get[Int](effectiveKey).get.get
        bytesValue2 shouldBe otherValue
      }
    }
  }
}

object CachedSpec {

  val Expire = 1.day

  case class Key(id: Long)
  case class Value(key: Key)
}
