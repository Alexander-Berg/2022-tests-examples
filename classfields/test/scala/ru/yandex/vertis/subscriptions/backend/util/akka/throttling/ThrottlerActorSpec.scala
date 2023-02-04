package ru.yandex.vertis.subscriptions.backend.util.akka.throttling

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/** Specs on [[ThrottlerActor]]
  */
@RunWith(classOf[JUnitRunner])
class ThrottlerActorSpec
  extends TestKit(ActorSystem("unit-test", ConfigFactory.parseString("")))
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  private val blunter = system.actorOf(Props(new TimeBluntActor(1.second)), "blunter")

  private val throttledBlunter = system.actorOf(
    Props(new ThrottlerActor(2, blunter)),
    "throttled-blunter"
  )

  "ThrottlerActor" should {
    "skip extra message" in {
      val sender = TestProbe()

      throttledBlunter.tell("foo", sender.ref)
      throttledBlunter.tell("bar", sender.ref)
      throttledBlunter.tell("baz", sender.ref)

      sender.expectMsg(ThrottlerActor.Overloaded("baz"))
      sender.expectMsgAllOf(3.seconds, "foo", "bar")
    }
  }

  override protected def afterAll() {
    Await.ready(system.terminate(), 10.seconds)
  }
}
