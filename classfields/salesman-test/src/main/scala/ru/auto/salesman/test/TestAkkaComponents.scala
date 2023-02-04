package ru.auto.salesman.test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

// trait just for dealing with initialization issues in trait-based tests
// prefer to use object when you can please
trait TestAkkaComponents {

  // empty config to avoid parsing it from application.conf
  implicit val system: ActorSystem = ActorSystem("test", ConfigFactory.empty)

  implicit val materializer: ActorMaterializer = ActorMaterializer()
}

object TestAkkaComponents extends TestAkkaComponents
