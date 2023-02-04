package vertis.statist.dao.counter

import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.statist.dao.counter.CachingCounterDao.{CacheValue, CounterCacheKey, CounterMultiCacheKey}
import org.joda.time.{DateTime, LocalDate}
import ru.yandex.vertis.generators.ProducerProvider
import vertis.statist.dao.DummyCounterDao
import vertis.statist.{Generators, TestAkkaSupport}
import vertis.statist.model.{Component, DatesPeriod, Domain, Id, ObjectCounterValues, ObjectDailyValues}
import vertis.zio.cache.ZioAsyncCache
import vertis.zio.test.{ZioEventually, ZioSpecBase}

import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import zio._
import zio.clock.Clock

/** @author zvez
  */
class CachingCounterDaoSpec extends ZioSpecBase with ProducerProvider with TestAkkaSupport with ZioEventually {

  private val default = 1

  trait CommonMixin {
    def domain: Domain = Domain("test")

    def backend: String = "test"

    def counterName: String = "test"

    def prometheusRegistry: PrometheusRegistry = TestOperationalSupport.prometheusRegistry
  }

  class Test(loadLatency: Long = 0L, cacheLatency: Long = 0L, failing: Boolean = false) {
    private val baseDao = new DummyCounterDao(default, loadLatency, failing)

    private val cacheTtl = 5.minutes

    val cachingDao =
      new CounterDaoWrapper(baseDao) with CachingCounterDao with CommonMixin {

        override val clock = Clock.Service.live

        override protected def loadTimeout: FiniteDuration =
          if (loadLatency > 0L) {
            (loadLatency.toDouble / 2).millis
          } else {
            super.loadTimeout
          }

        override protected def cacheTimeout: FiniteDuration =
          if (cacheLatency > 0L) {
            (cacheLatency.toDouble / 2).millis
          } else {
            super.cacheTimeout
          }

        override val counterCache =
          ZioAsyncCache.create(
            new InMemoryAsyncCache(CachingCounterDao.counterCacheLayout) {

              override def multiGet(
                  keys: Set[CounterCacheKey]
                )(implicit ec: ExecutionContext): Future[Map[CounterCacheKey, CacheValue[Int]]] =
                Future(Thread.sleep(cacheLatency))
                  .flatMap(_ => super.multiGet(keys))
            }
          )

        override val multiComponentCache =
          ZioAsyncCache.create(
            new InMemoryAsyncCache(CachingCounterDao.multiCacheLayout)
          )

        override val byDayCache =
          ZioAsyncCache.create(
            new InMemoryAsyncCache(CachingCounterDao.byDayCacheLayout)
          )

        def getFromCache(
            component: Component,
            ids: Set[Id],
            period: DatesPeriod): Task[Map[Id, Int]] =
          counterCache
            .multiGet(
              ids.map(CounterCacheKey(component, _, period))
            )
            .map(_.map { case (k, v) => k.id -> v.value })

        def putToCache(
            component: Component,
            ids: Set[Id],
            period: DatesPeriod,
            value: CacheValue[Int]): Task[Unit] =
          counterCache
            .multiSet(
              ids.map(CounterCacheKey(component, _, period) -> value).toMap,
              cacheTtl
            )

        def getFromMultiCache(
            components: Set[Component],
            ids: Set[Id],
            period: DatesPeriod): Task[Map[Id, ObjectCounterValues]] =
          multiComponentCache
            .multiGet(
              ids.map(CounterMultiCacheKey(components, _, period))
            )
            .map(_.map { case (k, v) => k.id -> v.value })

        def getFromDayCache(
            components: Set[Component],
            ids: Set[Id],
            period: DatesPeriod): Task[Map[Id, ObjectDailyValues]] =
          byDayCache
            .multiGet(
              ids.map(CounterMultiCacheKey(components, _, period))
            )
            .map(_.map { case (k, v) => k.id -> v.value })
      }
  }

  "CachingCounterDao" when {
    "getMultiple" should {
      val component = "some"

      "load what is not cached and cache loaded" in
        new Test {
          val id = Generators.Id.next
          val ids = Generators.Ids.next
          val all = ids + id

          ioTest {
            for {
              _ <- cachingDao.getFromCache(component, Set(id), DatesPeriod.Open).map(_ shouldBe Map.empty)
              _ <- cachingDao.getFromCache(component, ids, DatesPeriod.Open).map(_ shouldBe Map.empty)
              _ <- cachingDao.getMultiple(component, Set(id)).map(_.values shouldBe Map(id -> default))
              _ <- checkEventually {
                cachingDao.getFromCache(component, Set(id), DatesPeriod.Open).map(_ shouldBe Map(id -> default))
              }
              _ <- cachingDao.getMultiple(component, ids).map(_.values shouldBe ids.map(_ -> default).toMap)
              _ <- checkEventually {
                cachingDao.getFromCache(component, all, DatesPeriod.Open).map(_ shouldBe all.map(_ -> default).toMap)
              }
            } yield ()
          }
        }

      "not use outdated" in new Test {
        val id = Generators.Id.next

        ioTest {
          for {
            _ <- cachingDao.putToCache(component, Set(id), DatesPeriod.Open, CacheValue(10, DateTime.now))
            _ <- cachingDao.getMultiple(component, Set(id)).map(_.values shouldBe Map(id -> 10))

            _ <- cachingDao.putToCache(component, Set(id), DatesPeriod.Open, CacheValue(-1, DateTime.now.minusHours(1)))
            _ <- checkEventually {
              cachingDao.getMultiple(component, Set(id)).map(_.values shouldBe Map(id -> default))
            }
          } yield ()
        }

      }

      "use outdated if loading takes too long" in new Test(loadLatency = 200) {
        val id = Generators.Id.next

        ioTest {
          for {
            _ <- cachingDao.putToCache(component, Set(id), DatesPeriod.Open, CacheValue(-1, DateTime.now.minusHours(1)))
            _ <- cachingDao.getMultiple(component, Set(id)).map(_.values shouldBe Map(id -> -1))
          } yield ()
        }
      }

      "use outdated if loading fails" in new Test(failing = true) {
        val id = Generators.Id.next

        ioTest {
          for {
            _ <- cachingDao.putToCache(component, Set(id), DatesPeriod.Open, CacheValue(-1, DateTime.now.minusHours(1)))
            _ <- cachingDao.getMultiple(component, Set(id)).map(_.values shouldBe Map(id -> -1))
          } yield ()
        }
      }

      "load values if getting cached takes too long" in new Test(cacheLatency = 200) {
        val id = Generators.Id.next

        ioTest {
          for {
            _ <- cachingDao.putToCache(component, Set(id), DatesPeriod.Open, CacheValue(10, DateTime.now))
            _ <- cachingDao.getMultiple(component, Set(id)).map(_.values shouldBe Map(id -> 1))
          } yield ()
        }

      }
    }

    "getMultipleComponents" should {
      "load what is not cached and cache loaded" in new Test {
        val components = Set("a", "b")
        val objCounters = ObjectCounterValues(components.map(_ -> default).toMap)
        val id = Generators.Id.next
        val ids = Generators.Ids.next
        val all = ids + id

        ioTest {
          for {
            _ <- cachingDao.getFromMultiCache(components, Set(id), DatesPeriod.Open).map(_ shouldBe Map.empty)
            _ <- cachingDao.getFromMultiCache(components, ids, DatesPeriod.Open).map(_ shouldBe Map.empty)
            _ <- cachingDao.getMultipleComponents(components, Set(id)).map(_.byObject shouldBe Map(id -> objCounters))
            _ <- checkEventually {
              cachingDao
                .getFromMultiCache(components, Set(id), DatesPeriod.Open)
                .map(
                  _ shouldBe Map(
                    id -> objCounters
                  )
                )
            }
            _ <- cachingDao
              .getMultipleComponents(components, ids)
              .map(_.byObject shouldBe ids.map(_ -> objCounters).toMap)

            _ <- checkEventually {
              cachingDao
                .getFromMultiCache(components, all, DatesPeriod.Open)
                .map(
                  _ should contain theSameElementsAs all
                    .map(_ -> objCounters)
                    .toMap
                )
            }
          } yield ()
        }
      }
    }

    "getMultipleComponentsByDay" should {
      "load what is not cached and cache loaded" in new Test {
        val components = Set("a", "b")
        val objCounters =
          ObjectDailyValues(Map(LocalDate.now -> ObjectCounterValues(components.map(_ -> default).toMap)))
        val id = Generators.Id.next
        val ids = Generators.Ids.next
        val all = ids + id

        ioTest {
          for {
            _ <- cachingDao.getFromDayCache(components, Set(id), DatesPeriod.Open).map(_ shouldBe Map.empty)
            _ <- cachingDao.getFromDayCache(components, ids, DatesPeriod.Open).map(_ shouldBe Map.empty)
            _ <- cachingDao
              .getMultipleComponentsByDay(components, Set(id), DatesPeriod.Open)
              .map(_.byObject shouldBe Map(id -> objCounters))
            _ <- checkEventually {
              cachingDao
                .getFromDayCache(components, Set(id), DatesPeriod.Open)
                .map(_ shouldBe Map(id -> objCounters))
            }
            _ <- cachingDao
              .getMultipleComponentsByDay(components, ids, DatesPeriod.Open)
              .map(
                _.byObject shouldBe ids
                  .map(_ -> objCounters)
                  .toMap
              )
            _ <- checkEventually {
              cachingDao
                .getFromDayCache(components, all, DatesPeriod.Open)
                .map(
                  _ shouldBe all
                    .map(_ -> objCounters)
                    .toMap
                )
            }
          } yield ()
        }

      }
    }
  }
}
