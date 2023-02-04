package ru.yandex.vertis.passport

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Suite}

/**
  *
  * @author zvez
  */
trait AkkaSupport extends BeforeAndAfterAll { this: Suite =>

  implicit lazy val actorSystem = ActorSystem(getClass.getSimpleName)

  implicit lazy val materializer = ActorMaterializer()

  override protected def afterAll() = {
    super.afterAll()
    TestKit.shutdownActorSystem(actorSystem)
  }
}
