package ru.yandex.vertis.subscriptions.backend.transport.push

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.push.PushResponse
import ru.yandex.vertis.subscriptions.Mocking
import ru.yandex.vertis.subscriptions.backend.NotifierModelGenerators
import ru.yandex.vertis.subscriptions.backend.transport.push.PusherActor.{Request, Response}

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.Success

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class PusherActorSpec
  extends TestKit(ActorSystem("PusherActorSpec", ConfigFactory.empty()))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll
  with Mocking
  with ProducerProvider {

  "PusherActor" should {
    "work" in {
      val pushRequest = NotifierModelGenerators.pushRequest.next
      val pushResponse = NotifierModelGenerators.pushResponse.next
      val pusher = mock[Pusher]
      val actor = system.actorOf(
        Props(new PusherActor(pusher))
      )

      (pusher.sendNotification(_: PushRequest)).expects(pushRequest).returnsF(pushResponse)

      val notification = NotifierModelGenerators.formedNotification.next
      val request = Request(notification.key.subscription, notification, pushRequest)
      actor ! request

      expectMsg(Response(request, Success(pushResponse)))
    }

    "limit in flight messages" in {
      val pushRequest1 = NotifierModelGenerators.pushRequest.next
      val pushRequest2 = NotifierModelGenerators.pushRequest.next
      val pushResponse = NotifierModelGenerators.pushResponse.next

      val response1Promise = Promise[PushResponse]

      val pusher = mock[Pusher]
      val actor = system.actorOf(
        Props(new PusherActor(pusher, maxInFlight = 1))
      )
      (pusher.sendNotification(_: PushRequest)).expects(pushRequest1).returns(response1Promise.future)
      (pusher.sendNotification(_: PushRequest)).expects(pushRequest2).returnsF(pushResponse)

      val notification = NotifierModelGenerators.formedNotification.next
      val request1 = Request(notification.key.subscription, notification, pushRequest1)
      actor ! request1
      val request2 = Request(notification.key.subscription, notification, pushRequest2)
      actor ! request2

      expectNoMessage(5.seconds)

      response1Promise.success(pushResponse)
      expectMsg(Response(request1, Success(pushResponse)))
      expectMsg(Response(request2, Success(pushResponse)))
    }
  }

}
