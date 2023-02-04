package ru.yandex.vertis.subscriptions.api.v1.subscriptions.service.token

import akka.testkit.{TestActorRef, TestProbe}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.api.{DomainExceptionHandler, RouteTestWithConfig}
import ru.yandex.vertis.subscriptions.storage.SubscriptionsDaoActor.{Command, Request, Response, Result}
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.Mocking
import ru.yandex.vertis.subscriptions.storage.{SubscriptionsDao, TokenSubscriptions}
import ru.yandex.vertis.subscriptions.util.test.TestData

import spray.http.StatusCodes

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

/**
  * Specs on [[ru.yandex.vertis.subscriptions.api.v1.subscriptions.service.token.Handler]]
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends Matchers with WordSpecLike with RouteTestWithConfig with DomainExceptionHandler with Mocking {

  private val daoActorProbe = new TestProbe(system) {

    def respondTokens() = {
      expectMsgPF(3 seconds) {
        case req @ Request(Command.GetTokens, _) =>
          sender ! Response(req, Success(Result.SupportedTokens(new IntTokens(10))))
      }
    }

    def respondByToken() = {
      expectMsgPF(3 seconds) {
        case req @ Request(Command.ByToken(_), _) =>
          val result = Iterable(
            TestData.subscription("foo"),
            TestData.subscription("baz")
          )
          sender ! Response(req, Success(Result.RetrievedMulti(result)))
      }
    }
  }

  trait TestSubscriptionsDao extends SubscriptionsDao with TokenSubscriptions

  trait Test {
    val dao = mock[TestSubscriptionsDao]

    val route = seal {
      TestActorRef(new Handler(dao)).underlyingActor.route
    }
  }

  "GET /" should {
    "respond route to tokens" in new Test {
      (dao.tokens _).expects().returns(new IntTokens(10))
      Get("/") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[String].contains("1"))
      }
    }
  }
  "GET /1" should {
    "respond with subscriptions" in new Test {
      val result = Iterable(
        TestData.subscription("foo"),
        TestData.subscription("baz")
      )
      (dao.getWith _).expects("1").returnsF(result)
      Get("/1") ~> route ~> check {
        status should be(StatusCodes.OK)
        assert(responseAs[String].contains("foo"))
      }
    }
  }
}
