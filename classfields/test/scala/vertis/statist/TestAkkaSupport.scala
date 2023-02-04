package vertis.statist

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Suite}

/** @author zvez
  */
trait TestAkkaSupport extends BeforeAndAfterAll { this: Suite =>

  implicit val actorSystem: ActorSystem = ActorSystem(getClass.getSimpleName)

  override protected def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(actorSystem)
  }
}
