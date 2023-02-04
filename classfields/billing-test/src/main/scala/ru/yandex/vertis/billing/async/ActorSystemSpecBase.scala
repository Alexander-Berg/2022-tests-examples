package ru.yandex.vertis.billing.async

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory

/**
  * @author ruslansd
  */
trait ActorSystemSpecBase {

  protected def name: String

  implicit protected lazy val actorSystem: ActorSystem =
    ActorSystem(name, config = ConfigFactory.empty())

  implicit protected lazy val materializer: Materializer =
    Materializer(actorSystem)

}
