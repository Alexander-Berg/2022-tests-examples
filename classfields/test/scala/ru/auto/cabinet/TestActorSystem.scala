package ru.auto.cabinet

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

trait TestActorSystem {

  implicit protected val system =
    ActorSystem("test-system", ConfigFactory.empty())
}
