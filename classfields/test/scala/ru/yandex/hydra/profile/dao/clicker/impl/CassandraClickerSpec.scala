package ru.yandex.hydra.profile.dao.clicker.impl

import java.util.Random
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestKit}
import ru.yandex.hydra.profile.dao.actor.{IncrementDaoDispatcher, PerUserIncrementDao}
import ru.yandex.hydra.profile.dao.cassandra.TestCassandra
import ru.yandex.hydra.profile.dao.clicker.ClickerDAO
import ru.yandex.hydra.profile.dao.clicker.service.ClickerService
import ru.yandex.hydra.profile.dao.clicker.service.impl.ClickerServiceImpl
import ru.yandex.hydra.profile.dao.limiter.service.InMemoryCassandraLimiter
import ru.yandex.hydra.profile.dao.{IncrementDaoImpl, SpecBase}

import scala.concurrent._
import scala.concurrent.duration._

/** Unit tests for [[Cassandra]]
  *
  * @author incubos
  */
class CassandraClickerSpec extends TestKit(ActorSystem("CassandraClickerSpec")) with SpecBase with TestCassandra {

  def result[T](awaitable: Awaitable[T]): T =
    Await.result(awaitable, 5.seconds)

  private val IdPrefix: String = "id"
  val ID = IdPrefix + math.abs(new Random().nextInt())
  val Limit = 1000
  val count = Limit / 10

  private val expire: FiniteDuration = new FiniteDuration(1, TimeUnit.SECONDS)

  def c: ClickerService = {
    val unlimitedDao: ClickerDAO = new ClickerDAO {
      val storage = new InMemoryCassandraLimiter(1, TimeUnit.SECONDS)

      override protected def scope: String = "some"

      override def count(objectId: String)(implicit executionContext: ExecutionContext): Future[Int] = {
        storage
          .get(objectId)
          .map(o => {
            if (o > Limit) Limit else o
          })
      }

      override def dump(
          user: String,
          timeCounter: Int,
          value: Int
        )(implicit executionContext: ExecutionContext): Future[Unit] =
        storage.dump(user, timeCounter, value)
    }
    new ClickerServiceImpl(
      unlimitedDao,
      new IncrementDaoImpl(
        TestActorRef(
          IncrementDaoDispatcher.props(
            PerUserIncrementDao
              .props(unlimitedDao, 1, TimeUnit.SECONDS)
              .withDispatcher(CallingThreadDispatcher.Id)
          )
        )
      )
    )
  }

  "Cassandra" should {
    implicit val ec = new ExecutionContext {
      override def reportFailure(cause: Throwable): Unit = {}

      override def execute(runnable: Runnable): Unit = runnable.run()
    }
    "register hit #1" in {
      val clicker = c
      result(clicker.count(ID)) should equal(0)
      result(clicker.increment(ID))
      result(clicker.count(ID)) should equal(1)
    }

    "register hit #2" in {
      val clicker = c
      result(clicker.count(ID)) should equal(0)
      result(clicker.increment(ID))
      result(clicker.increment(ID))
      result(clicker.count(ID)) should equal(2)
    }

    s"register $count hits" in {
      val clicker = c
      result(Future.sequence((0 until count).map(i => clicker.increment(ID))))
      result(clicker.count(ID)) should equal(count)
    }

    s"limit hits" in {
      val clicker = c
      result(Future.sequence((0 until 2 * Limit).map(i => clicker.increment(ID))))
      result(clicker.count(ID)) should equal(Limit)
    }

    s"expire hit" in {
      val clicker = c
      result(clicker.increment(ID))
      Thread.sleep(expire.mul(2).toMillis)
      result(clicker.count(ID)) should equal(0)
    }

    "fail empty multi ID" in {
      val clicker = c
      intercept[IllegalArgumentException](result(clicker.count(Set.empty[String])))
    }

    "get nonexistent multi ID" in {
      val clicker = c
      val ID = IdPrefix + math.abs(new Random().nextInt())
      result(clicker.count(Set(ID))) should equal(Map(ID -> 0))
    }

    "get one multi ID" in {
      val clicker = c
      val ID = IdPrefix + math.abs(new Random().nextInt())
      result(clicker.increment(ID))
      val count1 = clicker.count(Set(ID))
      result(count1) should equal(Map(ID -> 1))
    }

    "get two multi IDs" in {
      val clicker = c
      val ID1 = IdPrefix + math.abs(new Random().nextInt())
      result(clicker.increment(ID1))
      val ID2 = IdPrefix + math.abs(new Random().nextInt())
      result(clicker.increment(ID2))
      result(clicker.increment(ID2))
      result(clicker.count(Set(ID1, ID2))) should equal(Map(ID1 -> 1, ID2 -> 2))
    }

    "get two multi IDs and nonexistent ID" in {
      val clicker = c
      val ID1 = IdPrefix + math.abs(new Random().nextInt())
      result(clicker.increment(ID1))
      val ID2 = IdPrefix + math.abs(new Random().nextInt())
      result(clicker.increment(ID2))
      result(clicker.increment(ID2))
      val ID3 = IdPrefix + math.abs(new Random().nextInt())
      result(clicker.count(Set(ID1, ID2, ID3))) should equal(Map(ID1 -> 1, ID2 -> 2, ID3 -> 0))
    }
  }
}
