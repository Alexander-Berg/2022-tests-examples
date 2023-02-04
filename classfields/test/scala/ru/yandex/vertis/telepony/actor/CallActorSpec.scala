package ru.yandex.vertis.telepony.actor

import akka.Done
import akka.actor.{ActorRef, ActorSystem, ReceiveTimeout}
import akka.pattern.ask
import akka.testkit.TestKit
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.Specbase
import ru.yandex.vertis.telepony.actor.CallsActor.Signal
import ru.yandex.vertis.telepony.generator.Generator.RawCallGen
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.EventModelGenerators._
import ru.yandex.vertis.telepony.model.beeline.BeelineCallEvent
import ru.yandex.vertis.telepony.model.mtt.MttCallEvent
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.impl.RawCallMonitorService
import ru.yandex.vertis.telepony.service.{CallbackBuilder, RawAppBackCallMonitorService, RawAppCallMonitorService, RawCallBuilder}

import scala.concurrent.Future
import scala.concurrent.duration._

class CallActorSpec
  extends TestKit(ActorSystem.create("call", ConfigFactory.empty()))
  with Specbase
  with MockitoSupport {

  implicit private val timeout: Timeout = 100.millis

  trait Test {
    val mockedJournal = mock[WriteJournal[RawCall]]
    val mockedCallbackJournal = mock[WriteJournal[RawCallback]]
    val mockedAppCallJournal = mock[WriteJournal[RawAppCall]]
    val mockedMtsBuilder = mock[RawCallBuilder[MtsCallEventAction]]
    val mockedVoxBuilder = mock[RawCallBuilder[VoxCallEventAction]]
    val mockedBeelineBuilder = mock[RawCallBuilder[BeelineCallEvent]]
    val mockedMttBuilder = mock[RawCallBuilder[MttCallEvent]]
    val mockedCallbackBuilder = mock[CallbackBuilder]
    val mockedMonitor = mock[RawCallMonitorService]
    val mockedAppMonitor = mock[RawAppCallMonitorService]
    val rawAppBackCallMonitor = mock[RawAppBackCallMonitorService]
    val appBackCallJournal = mock[WriteJournal[RawAppBackCall]]

    val actor: ActorRef = system.actorOf(
      CallActor.props(
        journal = mockedJournal,
        callbackJournal = mockedCallbackJournal,
        rawCallMonitor = mockedMonitor,
        mtsCallEventActionBuilder = mockedMtsBuilder,
        voxCallEventActionBuilder = mockedVoxBuilder,
        beelineBuilder = mockedBeelineBuilder,
        mttBuilder = mockedMttBuilder,
        voxCallbackBuilder = mockedCallbackBuilder,
        receiveTimeout = 1.minute,
        appCallJournal = mockedAppCallJournal,
        rawAppCallMonitor = mockedAppMonitor,
        rawAppBackCallMonitor = rawAppBackCallMonitor,
        appBackCallJournal = appBackCallJournal
      )
    )
  }

  "CallActor" should {
    "Mts - terminate on ReceiveTimeout" in new Test {
      val eventAction = MtsCallEventActionGen.next
      when(mockedMtsBuilder.build(Seq(eventAction))).thenReturn(None)
      when(mockedMonitor.markLost(?)).thenAnswer(_ => ())
      watch(actor)
      (actor ? Signal(eventAction)).mapTo[Done].futureValue
      actor ! ReceiveTimeout
      expectTerminated(actor)
      Mockito.verify(mockedMtsBuilder).build(Seq(eventAction))
      Mockito.verify(mockedMonitor).markLost(?)
    }

    "Mts - respond on receive event" in new Test {
      val eventAction = MtsCallEventActionGen.next
      val response = (actor ? Signal(eventAction)).mapTo[Done].futureValue
      response should ===(Done)
    }

    "Mts - respond when call built lost" in new Test {
      watch(actor)
      val eventAction = MtsCallEventActionGen.next
      (actor ? Signal(eventAction)).mapTo[Done].futureValue
      when(mockedMtsBuilder.build(Seq(eventAction))).thenReturn(None)
      when(mockedMonitor.markLost(?)).thenAnswer(_ => ())
      actor ! ReceiveTimeout
      expectTerminated(actor)
      Mockito.verify(mockedMtsBuilder).build(Seq(eventAction))
      Mockito.verify(mockedMonitor).markLost(?)
    }

    "Mts - respond when call built success" in new Test {
      watch(actor)
      val eventAction = MtsCallEventActionGen.next
      val call = RawCallGen.next
      (actor ? Signal(eventAction)).mapTo[Done].futureValue
      when(mockedMtsBuilder.build(Seq(eventAction))).thenReturn(Some(call))
      when(mockedJournal.send(call)).thenReturn(Future.successful(null))
      when(mockedMonitor.markSuccess(call)).thenAnswer(_ => ())
      actor ! ReceiveTimeout
      expectTerminated(actor)
      Mockito.verify(mockedMtsBuilder).build(Seq(eventAction))
      Mockito.verify(mockedJournal).send(call)
      eventually {
        Mockito.verify(mockedMonitor).markSuccess(call)
      }
    }

    "Vox - terminate on ReceiveTimeout" in new Test {
      val eventAction = VoxCallEventActionGen.next
      when(mockedVoxBuilder.build(Seq(eventAction))).thenReturn(None)
      when(mockedMonitor.markLost(?)).thenAnswer(_ => ())
      watch(actor)
      (actor ? Signal(eventAction)).mapTo[Done].futureValue
      actor ! ReceiveTimeout
      expectTerminated(actor)
      Mockito.verify(mockedVoxBuilder).build(Seq(eventAction))
      Mockito.verify(mockedMonitor).markLost(?)
    }

    "Vox - respond on receive event" in new Test {
      val eventAction = VoxCallEventActionGen.next
      val response = (actor ? Signal(eventAction)).mapTo[Done].futureValue
      response should ===(Done)
    }

    "Vox - respond when call built lost" in new Test {
      watch(actor)
      val eventAction = VoxCallEventActionGen.next
      (actor ? Signal(eventAction)).mapTo[Done].futureValue
      when(mockedVoxBuilder.build(Seq(eventAction))).thenReturn(None)
      when(mockedMonitor.markLost(?)).thenAnswer(_ => ())
      actor ! ReceiveTimeout
      expectTerminated(actor)
      Mockito.verify(mockedVoxBuilder).build(Seq(eventAction))
      Mockito.verify(mockedMonitor).markLost(?)
    }

    "Vox - respond when call built success" in new Test {
      watch(actor)
      val eventAction = VoxCallEventActionGen.next
      val call = RawCallGen.next
      (actor ? Signal(eventAction)).mapTo[Done].futureValue
      when(mockedVoxBuilder.build(Seq(eventAction))).thenReturn(Some(call))
      when(mockedJournal.send(call)).thenReturn(Future.successful(null))
      when(mockedMonitor.markSuccess(call)).thenAnswer(_ => ())
      actor ! ReceiveTimeout
      expectTerminated(actor)
      Mockito.verify(mockedVoxBuilder).build(Seq(eventAction))
      Mockito.verify(mockedJournal).send(call)
      eventually {
        Mockito.verify(mockedMonitor).markSuccess(call)
      }
    }
  }

}
