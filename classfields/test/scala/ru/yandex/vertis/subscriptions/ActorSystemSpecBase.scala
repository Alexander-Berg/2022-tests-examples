package ru.yandex.vertis.subscriptions

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.util.Logging

abstract class ActorSystemSpecBase(_system: ActorSystem)
  extends TestKit(_system)
  with WordSpecLike
  with Matchers
  with Logging
  with ImplicitSender
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

}
