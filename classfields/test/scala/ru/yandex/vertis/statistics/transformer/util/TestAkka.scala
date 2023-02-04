package ru.yandex.vertis.statistics.transformer.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Suite}

/**
  *
  * @author zvez
  */
trait TestAkka extends BeforeAndAfterAll { this: Suite =>

  implicit val actorSystem: ActorSystem = ActorSystem(getClass.getSimpleName, ConfigFactory.empty())
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override protected def afterAll(): Unit = {
    materializer.shutdown()
    actorSystem.terminate()
    super.afterAll()
  }
}
