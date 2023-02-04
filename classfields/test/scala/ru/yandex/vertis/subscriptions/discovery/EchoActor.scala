package ru.yandex.vertis.subscriptions.discovery

import akka.actor.Actor

/** Actor for testing purposes
  */
class EchoActor extends Actor {

  def receive = {
    case x => sender ! x
  }
}
