package ru.yandex.vertis.subscriptions

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.util.Logging

abstract class ActorSpecBase(_system: ActorSystem)
  extends TestKit(_system)
  with WordSpecLike
  with Matchers
  with ImplicitSender
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Logging {

  def this() = this(ActorSystem("DefaultActorSpecSystem"))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

}
