package ru.yandex.vertis.passport.concurrent

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.vertis.passport.util.concurrent.Futures.RichFuture

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future, TimeoutException}

/**
  *
  * @author zvez
  */
class RichFutureSpec
  extends TestKit(ActorSystem("test"))
  with WordSpecLike
  with ScalaFutures
  with Matchers
  with BeforeAndAfterAll {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "RichFuture.withTimeout" should {
    "work" in {
      Future {
        "ok"
      }.withTimeout(1.second).futureValue shouldBe "ok"
    }

    "throw TimeoutException on timeout" in {
      val f = Future.never.withTimeout(1.second)
      Await.ready(f, 2.seconds)
      f.failed.futureValue shouldBe a[TimeoutException]
    }
  }
}
