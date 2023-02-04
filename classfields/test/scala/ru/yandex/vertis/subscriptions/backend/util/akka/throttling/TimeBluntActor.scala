package ru.yandex.vertis.subscriptions.backend.util.akka.throttling

import akka.actor.Actor
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.concurrent.duration.FiniteDuration

/**
  * Sends received messages to sender after given timeout
  */
@RunWith(classOf[JUnitRunner])
class TimeBluntActor(bluntTime: FiniteDuration) extends Actor {

  def receive = {
    case msg =>
      Thread.sleep(bluntTime.toMillis)
      sender() ! msg
  }
}
