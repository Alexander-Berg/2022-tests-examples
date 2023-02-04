package ru.yandex.vertis.passport.scheduler

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Suite}

/**
  *
  * @author zvez
  */
trait AkkaSupport extends BeforeAndAfterAll { this: Suite =>

  implicit lazy val actorSystem = ActorSystem(getClass.getSimpleName, ConfigFactory.empty())

  override protected def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(actorSystem)
  }
}
