package ru.yandex.hydra.profile.dao.limiter.service

import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestKit}
import ru.yandex.hydra.profile.dao.actor.{IncrementDaoDispatcher, PerUserIncrementDao}
import ru.yandex.hydra.profile.dao.limiter.dao.impl.LimiterDao
import ru.yandex.hydra.profile.dao.limiter.service.LimiterServiceSpec._
import ru.yandex.hydra.profile.dao.{CachedGetDao, IncrementDaoImpl, SpecBase}

import java.util.Random
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable, ExecutionContext}

/** @author @logab
  */
trait LimiterServiceSpec extends SpecBase {
  self: TestKit =>

  implicit val ec = new ExecutionContext {
    override def reportFailure(cause: Throwable): Unit = {}

    override def execute(runnable: Runnable): Unit = runnable.run()
  }

  def cachingDao: LimiterDao = {
    val storage = new InMemoryCassandraLimiter(Ttl, Time)
    new LimiterDao(
      new CachedGetDao(storage, CacheSize, CacheExpire),
      new IncrementDaoImpl(
        TestActorRef[IncrementDaoDispatcher](
          IncrementDaoDispatcher.props(
            PerUserIncrementDao
              .props(storage, Ttl, Time)
              .withDispatcher(CallingThreadDispatcher.Id)
          )
        )
      )
    )
  }

  def limiterDao: LimiterDao = {
    val storage = new InMemoryCassandraLimiter(Ttl, Time)
    new LimiterDao(
      storage,
      new IncrementDaoImpl(
        TestActorRef[IncrementDaoDispatcher](
          IncrementDaoDispatcher.props(
            PerUserIncrementDao
              .props(storage, Ttl, Time)
              .withDispatcher(CallingThreadDispatcher.Id)
          )
        )
      )
    )
  }

  def limiterService: LimiterService

  def result[A](awaitable: Awaitable[A]): A =
    Await.result(awaitable, 5000.seconds)

  val USER = "user" + math.abs(new Random().nextInt())

  "LimiterService" should {
    "limit user" in {
      val ls = limiterService
      (1 to Limit).foreach(request => {
        val res = result(ls.budgetOf(USER))
        res should equal(Limit - request + 1)
      })
    }

    "allow exactly a Limit number of calls" in {
      val ls = limiterService
      (1 until Limit).foreach(_ => {
        result(ls.budgetOf(USER))
      })
      val lastCall = result(ls.budgetOf(USER))
      lastCall shouldBe 1
      val noMore = result(ls.budgetOf(USER))
      noMore shouldBe 0
    }

    "not count any more" in {
      val ls = limiterService
      (1 to Limit * 2).foreach(_ => result(ls.budgetOf(USER)))
      result(ls.budgetOf(USER)) should equal(0)
    }

    "reset limits" in {
      val ls = limiterService
      (1 to Limit).foreach(_ => result(ls.budgetOf(USER)))
      Thread.sleep((Interval + 1.second).toMillis)
      result(ls.budgetOf(USER)) should equal(Limit)
    }
  }

}

object LimiterServiceSpec {

  val Limit = 1000
  val Ttl = 2
  val Time = TimeUnit.SECONDS
  val Interval = new FiniteDuration(Ttl, Time)
  val CacheSize = 10
  val CacheExpire = 1.second
}
