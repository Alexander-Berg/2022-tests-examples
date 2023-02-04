package ru.yandex.common.actor

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
 * @author @logab
 */
class BurstScalerImplSpec(_system: ActorSystem)
    extends TestKit(_system) with BurstScalerSpec {
  def this() = this(ActorSystem(
    "testsystem",
    ConfigFactory.parseString( """
akka.event-handlers = ["akka.testkit.TestEventListener"]
                               """)))

  override def scaler(probe: TestProbe, initialDelay: Boolean) =
    () =>
      new BurstScaler[Long](
      probe.ref,
      {
        case i: Long => Some(i)
        case _ => None
      },
      5,
      Some(1.second),
      randomizeInitialDelay = initialDelay
      )
}