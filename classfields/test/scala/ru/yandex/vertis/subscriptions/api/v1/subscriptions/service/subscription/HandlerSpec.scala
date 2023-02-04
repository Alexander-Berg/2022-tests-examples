package ru.yandex.vertis.subscriptions.api.v1.subscriptions.service.subscription

import ru.yandex.vertis.subscriptions.api.{DomainExceptionHandler, RouteTestWithConfig}
import ru.yandex.vertis.subscriptions.storage.SubscriptionsDao
import ru.yandex.vertis.subscriptions.util.test.TestData
import ru.yandex.vertis.subscriptions.{Mocking, SubscriptionId}

import akka.testkit.TestActorRef
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import spray.http._
import spray.routing.Route

import scala.concurrent.Future
import scala.language.postfixOps

/**
  * Specs on [[ru.yandex.vertis.subscriptions.api.v1.subscriptions.service.subscription.Handler]]
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends Matchers with WordSpecLike with RouteTestWithConfig with DomainExceptionHandler with Mocking {

  trait Test {
    val dao = mock[SubscriptionsDao]

    val route: Route = seal {
      TestActorRef(new Handler(dao)).underlyingActor.route
    }
  }

  "GET /foo" should {
    "respond with found subscription" in new Test {
      (dao.get(_: SubscriptionId)).expects("foo").returnsF(TestData.subscription("foo"))
      Get("/foo") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[String].contains("foo"))
      }
    }
  }

  "GET /bar" should {
    "respond NotFound" in new Test {
      (dao.get(_: SubscriptionId)).expects("bar").returns(Future.failed(new NoSuchElementException))
      Get("/bar") ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
  }

  "GET /failed" should {
    "respond InternalServerError" in new Test {
      (dao.get(_: SubscriptionId)).expects("failed").returns(Future.failed(new Exception("Artificial")))
      Get("/failed") ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }
  }

  "GET /failed" should {
    "respond BadRequest on IllegalArgumentException" in new Test {
      (dao
        .get(_: SubscriptionId))
        .expects("failed")
        .returns(Future.failed(new IllegalArgumentException("Artificial")))
      Get("/failed") ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }
  }
}
