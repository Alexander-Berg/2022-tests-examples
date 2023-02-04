package ru.yandex.common.discovery

import java.util.Collections
import java.util.concurrent.{ConcurrentHashMap, Phaser}

import akka.actor._
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.common.ZooKeeperAware
import ru.yandex.common.discovery.ActorServiceDiscovery.Event
import ru.yandex.common.discovery.ActorServiceDiscoveryIntSpec._

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.util.Success

/**
  * Specs on [[ru.yandex.common.discovery.ActorServiceDiscovery]]
  */
class ActorServiceDiscoveryIntSpec
  extends TestKit(ActorSystem("unit-test"))
    with WordSpecLike
    with Matchers
    with ZooKeeperAware {

  val curator = curatorBase.usingNamespace("root/of/service")

  case object Message

  "ActorServiceDiscovery" should {
    "register, delivery and unregister for single actor" in {
      registerDeliveryUnregister(1)
    }

    "register, delivery and unregister for 2 actors" in {
      registerDeliveryUnregister(2)
    }

    "register, delivery and unregister for 10 actors" in {
      registerDeliveryUnregister(10)
    }

    /*"register, delivery and unregister for 100 actors" in {
      registerDeliveryUnregister(100)
    }*/

    "neighbour ActorServiceDiscovery should discover ActorSelections" in {
      val DiscoveryId = "shared"

      val discovery1 = nextDiscovery(DiscoveryId)

      val ref = TestProbe().ref
      val registerWaiter = AwaitAddedListener(ref)
      discovery1.subscribe(registerWaiter)

      discovery1.register(ref)
      registerWaiter.await()

      val discovery2 = nextDiscovery(DiscoveryId)
      discovery2.instances should not be empty

      discovery1.close()
      discovery2.close()
    }
  }

  private def registerDeliveryUnregister(nrOfActors: Int) {
    val discovery = nextDiscovery(nrOfActors.toString)

    val probes = (1 to nrOfActors).map(i => TestProbe(s"probe-$i"))

    val selections = probes.
      map(p => system.actorSelection(p.ref.path)).
      toSet

    val registerWaiter = AwaitAddedListener(selections)

    discovery.subscribe(registerWaiter)

    probes foreach {
      probe =>
        discovery.register(probe.ref) should be(Success(true))
    }

    registerWaiter.await()

    discovery.instances should have size nrOfActors
    discovery.instances.foreach(as => as ! Message)

    probes foreach {
      probe => probe.expectMsg(Message)
    }

    discovery.unsubscribe(registerWaiter)

    val unregisterWaiter = AwaitRemovedListener(selections)

    discovery.subscribe(unregisterWaiter)

    probes foreach {
      probe =>
        discovery.unregister(probe.ref) should be(Success(true))
    }

    unregisterWaiter.await()

    discovery.unsubscribe(unregisterWaiter)

    discovery.instances should have size 0

    discovery.close()
  }

  private def nextDiscovery(id: String) =
    new ActorServiceDiscovery(
      curator,
      "base/path/to/discovery",
      s"test-service-$id",
      syncPeriod = 50.millis)
}

object ActorServiceDiscoveryIntSpec {

  class AwaitArrived(shouldArrive: Set[ActorSelection])
                    (isArrived: ActorServiceDiscovery.Event => Boolean)
    extends ActorServiceDiscovery.EventListener {

    val remain = newConcurrentSet(shouldArrive)
    val phaser = new Phaser(shouldArrive.size + 1)

    override def onEvent(e: ActorServiceDiscovery.Event) =
      if (isArrived(e)) {
        if (remain.remove(e.selection)) {
          phaser.arrive()
        }
      } else {
        if (remain.add(e.selection)) {
          phaser.register()
        }
      }

    def await(): Unit = {
      phaser.arriveAndAwaitAdvance()
    }
  }

  case class AwaitAddedListener(waitForAdd: Set[ActorSelection])
    extends AwaitArrived(waitForAdd)({
      case Event.Added(_) => true
      case Event.Removed(_) => false
    })

  object AwaitAddedListener {
    def apply(selection: ActorSelection): AwaitAddedListener =
      AwaitAddedListener(Set(selection))

    def apply(actorRef: ActorRef)
             (implicit arf: ActorRefFactory): AwaitAddedListener =
      apply(arf.actorSelection(actorRef.path))
  }

  case class AwaitRemovedListener(waitForRemove: Set[ActorSelection])
    extends AwaitArrived(waitForRemove)({
      case Event.Added(_) => false
      case Event.Removed(_) => true
    })

  object AwaitRemovedListener {
    def apply(selection: ActorSelection): AwaitRemovedListener =
      AwaitRemovedListener(Set(selection))

    def apply(actorRef: ActorRef)
             (implicit arf: ActorRefFactory): AwaitRemovedListener =
      apply(arf.actorSelection(actorRef.path))
  }

  private def newConcurrentSet[A](values: Set[A]) = {
    val map = new ConcurrentHashMap[A, java.lang.Boolean]()
    val result = Collections.newSetFromMap(map)
    result.addAll(values.asJavaCollection)
    result
  }

}
