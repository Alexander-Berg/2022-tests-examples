package ru.yandex.hydra.profile.dao.actor

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.LoggerFactory
import ru.yandex.hydra.profile.dao.actor.IncrementDaoDispatcher.Increment
import ru.yandex.hydra.profile.dao.actor.LimiterActorSpec._
import ru.yandex.hydra.profile.dao.limiter.service.InMemoryCassandraLimiter

/** @author @logab
  */
class LimiterActorSpec
  extends TestKit(ActorSystem("LimiterActorSpec"))
  with AnyWordSpecLike
  with Matchers
  with ScalaFutures {
  val log = LoggerFactory.getLogger(classOf[LimiterActorSpec])

  def limiterActor: TestActorRef[IncrementDaoDispatcher] = TestActorRef[IncrementDaoDispatcher](
    IncrementDaoDispatcher.props(PerUserIncrementDao.props(new InMemoryCassandraLimiter(Ttl, unit), Ttl, unit))
  )

  "LimiterActor" should {
    "create new per user processor" in {
      val limiter = limiterActor
      val u = user
      limiter ! Increment(u)
      limiter.children should have size 1
    }
    "create another per user processor" in {
      val limiter = limiterActor
      val u1 = user
      limiter ! Increment(u1)
      limiter.children should have size 1
      val u2 = user
      limiter ! Increment(u2)
      limiter.children should have size 2
    }
    "not create new per user processor" in {
      val limiter = limiterActor
      val u = user
      limiter ! Increment(u)
      limiter.children should have size 1
      limiter ! Increment(u)
      limiter.children should have size 1
    }
  }

}

object LimiterActorSpec {
  def user: String = "User" + Math.random()

  val Ttl = 2
  val unit = TimeUnit.SECONDS
}
