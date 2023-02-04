package ru.yandex.vertis.subscriptions.dc

import ru.yandex.vertis.subscriptions.Curator
import ru.yandex.vertis.subscriptions.util.Logging

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Success

/**
  * Specs on [[ru.yandex.vertis.subscriptions.dc.DataCenterCacheActor]]
  */
@RunWith(classOf[JUnitRunner])
class DataCenterCacheActorSpec
  extends TestKit(ActorSystem("unit-test", ConfigFactory.empty()))
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with Logging {

  private val curator = Curator.testingClient.usingNamespace("test")

  private val dataCenterCache = system.actorOf(Props(new DataCenterCacheActor(curator)), "test-data-center-cache")

  private val clientA = TestProbe()
  private val clientB = TestProbe()

  "DataCenterCacheActor" should {

    val timeout = 10.seconds
    val noMsgTimeout = 1.second

    "reply with Reply.CurrentState to Subscribe command" in {
      clientA.send(dataCenterCache, DataCenterCacheActor.Command.Subscribe)
      clientA.expectMsgAllClassOf(timeout, classOf[DataCenterCacheActor.Reply.CurrentState])
    }

    "reply with Reply.Added to Add command and send notification" in {
      val add = DataCenterCacheActor.Command.Add("fol", "csback01g.yandex.ru")
      clientB.send(dataCenterCache, add)

      val expectedAddResult = DataCenterCacheActor.Reply.Added(add, Success(()))
      clientB.expectMsg(timeout, expectedAddResult)

      clientA.expectMsgAllClassOf(timeout, classOf[DataCenterCacheActor.Notification.Added])
    }

    "reply with Reply.Added without notifications" in {
      val add = DataCenterCacheActor.Command.Add("fol", "csback01g.yandex.ru")
      clientB.send(dataCenterCache, add)

      val expectedAddResult = DataCenterCacheActor.Reply.Added(add, Success(()))
      clientB.expectMsg(timeout, expectedAddResult)

      clientA.expectNoMessage(noMsgTimeout)
    }

    "reply with Reply.Removed without notifications" in {
      val remove = DataCenterCacheActor.Command.Remove("not-exists-dc", "not-exists-host")
      clientB.send(dataCenterCache, remove)

      val expectedAddResult = DataCenterCacheActor.Reply.Removed(remove, Success(()))
      clientB.expectMsg(expectedAddResult)

      clientA.expectNoMessage(noMsgTimeout)
    }

    "correct play scenario" in {
      val add1 = DataCenterCacheActor.Command.Add("fol", "csback01g.yandex.ru")
      clientA.send(dataCenterCache, add1)
      clientA.expectMsgPF(timeout) {
        case DataCenterCacheActor.Reply.Added(_, Success(())) => true
      }

      clientB.send(dataCenterCache, DataCenterCacheActor.Command.Subscribe)
      clientB.expectMsgPF(timeout) {
        case DataCenterCacheActor.Reply.CurrentState(snapshot) if snapshot.nonEmpty => true
      }

      val add2 = DataCenterCacheActor.Command.Add("fol", "csback02g.yandex.ru")
      clientA.send(dataCenterCache, add2)
      clientA.expectMsgPF(timeout) {
        case DataCenterCacheActor.Reply.Added(_, Success(())) => true
      }

      clientB.expectNoMessage(noMsgTimeout)

      val remove1 = DataCenterCacheActor.Command.Remove("fol", "csback01g.yandex.ru")
      clientA.send(dataCenterCache, remove1)
      clientA.expectMsgPF(timeout) {
        case DataCenterCacheActor.Reply.Removed(_, Success(())) => true
      }

      clientB.expectNoMessage(noMsgTimeout)

      val remove2 = DataCenterCacheActor.Command.Remove("fol", "csback02g.yandex.ru")
      clientA.send(dataCenterCache, remove2)
      clientA.expectMsgPF(timeout) {
        case DataCenterCacheActor.Reply.Removed(_, Success(())) => true
      }

      clientB.expectMsgPF(timeout) {
        case DataCenterCacheActor.Notification.Removed("fol", snapshot) if snapshot.isEmpty => true
      }
    }
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }
}
