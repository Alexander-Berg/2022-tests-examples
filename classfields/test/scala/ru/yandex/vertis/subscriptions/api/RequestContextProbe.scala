package ru.yandex.vertis.subscriptions.api

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.routing.RequestContext

/** Helps to write test probes on different routers
  */
class RequestContextProbe(system: ActorSystem) extends TestProbe(system) {

  def expectHttp(check: RequestContext => Boolean, complete: RequestContext => Unit, timeout: Duration = 3 second) = {
    expectMsgPF(timeout) {
      case req: RequestContext if check(req) =>
        complete(req)
        true
    }
  }
}
