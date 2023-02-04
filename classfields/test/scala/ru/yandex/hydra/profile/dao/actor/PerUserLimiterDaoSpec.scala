package ru.yandex.hydra.profile.dao.actor

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props, Terminated}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.LoggerFactory
import ru.yandex.hydra.profile.dao.actor.PerUserIncrementDao.Increment
import ru.yandex.hydra.profile.dao.actor.PerUserLimiterDaoSpec._
import ru.yandex.hydra.profile.dao.limiter.service.InMemoryCassandraLimiter
import scala.concurrent.duration._

/** @author @logab
  */
class PerUserLimiterDaoSpec
  extends TestKit(ActorSystem("PerUserLimiterDaoSpec"))
  with AnyWordSpecLike
  with Matchers
  with ScalaFutures {
  val log = LoggerFactory.getLogger(classOf[PerUserLimiterDaoSpec])
  val storage: InMemoryCassandraLimiter = new InMemoryCassandraLimiter(Ttl, unit)

  def user: String = "User" + Math.random()

  import scala.concurrent.ExecutionContext.Implicits.global

  def actor(user: String): TestActorRef[PerUserIncrementDao] =
    TestActorRef[PerUserIncrementDao](Props(new PerUserIncrementDao(storage, Ttl, unit)), user)

  "PerUserLimiterDao" should {
    "increment" in {
      val u = user
      val perUserActor = actor(u)
      perUserActor ! Increment
      storage.get(u).futureValue shouldEqual 1
    }
    "increment several times" in {
      val u = user
      val perUserActor = actor(u)
      perUserActor ! Increment
      perUserActor ! Increment
      perUserActor ! Increment
      perUserActor ! Increment
      storage.get(u).futureValue shouldEqual 4
    }
    "increment in several ticks" in {
      val u = user
      val perUserActor = actor(u)
      perUserActor ! Increment
      perUserActor ! Increment
      Thread.sleep(TimeUnit.MILLISECONDS.convert(1, unit))
      perUserActor ! Increment
      perUserActor ! Increment
      storage.get(u).futureValue shouldEqual 4
    }
    "kill itself" in {
      val u = user
      val probe = TestProbe()
      val perUserActor = actor(u)
      probe.watch(perUserActor)
      perUserActor ! Increment
      probe.expectMsgClass(4.seconds, classOf[Terminated]) match {
        case Terminated(a) => a shouldEqual perUserActor
      }

    }
  }

}

object PerUserLimiterDaoSpec {
  val Ttl = 3
  val unit = TimeUnit.SECONDS
}
