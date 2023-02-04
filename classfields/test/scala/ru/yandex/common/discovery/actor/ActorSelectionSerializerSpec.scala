package ru.yandex.common.discovery.actor

import akka.actor.{ActorNotFound, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{Matchers, WordSpecLike}

import scala.util.Success

/**
 * Specs on [[ActorSelectionSerializer]]
 *
 * @author dimas
 */
class ActorSelectionSerializerSpec
  extends TestKit(ActorSystem("unit-test"))
  with WordSpecLike
  with Matchers {

  val serializer = new ActorSelectionSerializer(system)
  val probe = new TestProbe(system)

  "ActorSelectionSerializer" should {
    "round trip exists actor" in {
      val initial = system.actorSelection(probe.testActor.path)
      val serialized = serializer.serialize(initial)
      serializer.deserialize(serialized) should be(Success(initial))
    }

    "not deserialize non-exists actor" in {
      val initial = system.actorSelection("/foo/bar/baz")
      val serialized = serializer.serialize(initial)
      intercept[ActorNotFound] {
        serializer.deserialize(serialized).get
      }
    }
  }
}
