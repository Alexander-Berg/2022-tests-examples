package ru.yandex.common.actor

import akka.actor.{ActorSystem, DeadLetter}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * Tests for [[DeadLetterListener]]
 *
 * @author incubos
 */
class DeadLetterListenerSpec
    extends TestKit(ActorSystem("DeadLetterListenerSpec"))
            with ImplicitSender
            with WordSpecLike
            with Matchers
            with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "DeadLetterListener" should {
    "be defined for dead letters" in {
      TestActorRef(new DeadLetterListener).underlyingActor.receive(
        DeadLetter(1, testActor, testActor))
    }

    "be undefined for non dead letters" in {
      TestActorRef(new DeadLetterListener)
          .underlyingActor
          .receive
          .isDefinedAt(1) should be (false)
    }
  }
}
