package ru.yandex.common.tokenization

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.common.ZooKeeperAware
import ru.yandex.common.tokenization.OwnershipsKeeper._

/**
 * Specs on [[OwnershipsKeeper]]
 */
class OwnershipsKeeperIntSpec
  extends TestKit(ActorSystem("unit-test"))
  with Matchers
  with WordSpecLike
  with ZooKeeperAware {

  val log = LoggerFactory.getLogger(getClass)

  val tokens = new IntTokens(16)

  val curator = curatorBase.usingNamespace("ownerships-keeper-spec")
  var ownershipKeeper = system.actorOf(
    Props(new OwnershipsKeeper(curator, tokens)),
    "ownerships-keeper")

  override protected def afterAll() = {
    shutdown(system)
    super.afterAll()
  }

  // Will be removed soon
  "OwnershipsKeeper" should {

    import OwnershipsKeeper.Notification

    val subscriber = TestProbe()

    "handle Subscribe command multiple times" in {
      ownershipKeeper.tell(Command.Subscribe, subscriber.ref)
      subscriber.expectMsg(Notification(Event.Initialized, Ownerships.empty(tokens)))

      ownershipKeeper.tell(Command.Subscribe, subscriber.ref)
      subscriber.expectMsg(Notification(Event.Initialized, Ownerships.empty(tokens)))
    }

    "handle Acquire command and send notification" in {
      val ownership = Ownership(Owner("foo"), "1", 1)
      ownershipKeeper ! Command.Acquire(ownership)
      subscriber.expectMsg(Notification(Event.Acquired(ownership), Ownerships(Set(ownership), tokens)))
    }

    "handle Release command and send notification" in {
      val ownership = Ownership(Owner("foo"), "1", 1)
      ownershipKeeper ! Command.Release(ownership)
      subscriber.expectMsg(Notification(Event.Released(ownership), Ownerships.empty(tokens)))
    }

    "handle Steal command and send notification" in {
      val token = "1"
      val ownership = Ownership(Owner("foo"), token, 1)
      ownershipKeeper ! Command.Acquire(ownership)
      subscriber.expectMsg(Notification(Event.Acquired(ownership), Ownerships(Set(ownership), tokens)))

      val theft = Owner("bar")
      val acquired = Ownership(theft, token, 1)

      ownershipKeeper ! Command.Steal(ownership, Ownership(theft, token, 1))
      val notification = subscriber.expectMsgType[Notification]
      notification.event should be (Event.Released(ownership))
      // Cache may already process acquired event
      notification.ownerships should (be(Ownerships(Set(acquired), tokens)) or be(Ownerships.empty(tokens)))
      subscriber.expectMsg(Notification(Event.Acquired(acquired), Ownerships(Set(acquired), tokens)))
    }

    "handle Unsubscribe command and send notification" in {
      ownershipKeeper.tell(Command.Unsubscribe, subscriber.ref)

      val ownership = Ownership(Owner("baz"), "2")
      ownershipKeeper ! Command.Acquire(ownership)

      subscriber.expectNoMessage()
    }
  }
}
