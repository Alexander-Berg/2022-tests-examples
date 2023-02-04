package ru.yandex.vertis.util.akka.http.directives.throttling


import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import org.scalatest.concurrent.Eventually
import ru.yandex.vertis.util.akka.http.AkkaHttpSpecBase

import scala.concurrent.{ExecutionContextExecutor, Promise}

/**
  * @author @logab
  */
class ThrottlingDirectiveSpec extends AkkaHttpSpecBase with Eventually {
  val ec = new ExecutionContextExecutor {
    override def reportFailure(cause: Throwable): Unit = {}

    override def execute(command: Runnable): Unit = command.run()
  }
  abstract class Test(maxConcurrent: Int) extends ThrottlingDirective with Directives {

    @volatile
    var list = List.empty[Promise[Unit]]

    def complete(): Unit = list match {
      case Nil =>
      case x :: xs =>
        x.success(())
        list = xs
    }

    def roundTrip(request: HttpRequest): Unit = {
      val r = request ~> route
      complete()
      r ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    val failRoute: Route = _ => throw new UnsupportedOperationException("artificial")

    val route = throttled(maxConcurrent) {
      path("ok") {
        http =>
          val p = Promise[Unit]()
          list = list ::: List(p)
          p.future
              .flatMap { _ => http.complete("") }(ec)
              .recoverWith { case error => http.complete(error) }(ec)
      } ~ path("fail") {
        failRoute
      }
    }
  }

  "throttling directive" should {

    val okRequest = Get("http://test.test/ok")
    val rejectedRequest = Get("http://test.test/nonexistent")
    val failRequest = Get("http://test.test/fail")

    "throttle" in new Test(1) {
      okRequest ~> route
      okRequest ~> route ~> check {
        rejections.size should be > 0
      }
      complete()
    }
    "not stuck" in new Test(1) {
      roundTrip(okRequest)
      roundTrip(okRequest)
      roundTrip(okRequest)
      roundTrip(okRequest)
    }
    "not stuck on overflow" in new Test(1) {
      val result = okRequest ~> route
      eventually { list.size shouldBe 1 }
      okRequest ~> route ~> check { rejections.size should be > 0 }
      okRequest ~> route ~> check { rejections.size should be > 0 }
      complete()
      eventually { result.handled shouldBe true }
      roundTrip(okRequest)
    }
    "not stuck when unhandled exception occurs" in new Test(1) {
      failRequest ~> route ~> check(())
      roundTrip(okRequest)
    }

    "not stuck when unexpected rejection occurs" in new Test(1) {
      rejectedRequest ~> route ~> check(())
      roundTrip(okRequest)
    }
  }

}
