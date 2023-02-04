package ru.yandex.realty.event

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.junit.runner.RunWith
import org.scalatest.Assertion
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.auth.AuthInfo
import ru.yandex.realty.event.model.{VertisEvent, VertisRequestContext}
import ru.yandex.realty.event.model.VertisEvent._
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.platform.PlatformInfo
import ru.yandex.realty.pushnoy.PushnoyClient

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

@RunWith(classOf[JUnitRunner])
class VertisEventManagerTest extends SpecBase with RequestAware with ScalatestRouteTest {

  override def testConfig: Config = ConfigFactory.empty()
  override def marshallingTimeout: FiniteDuration = 1.second

  private val vertisEventSenderMock = mock[VertisEventSender]
  private val pushnoyClientMock = mock[PushnoyClient]

  private val manager = new VertisEventManager(vertisEventSenderMock, pushnoyClientMock)
  private val vertisRequestContext =
    VertisRequestContext.apply(platform = Some("android"), userId = Some("USER_ID_123"))
  private val authInfo = AuthInfo.apply(uidOpt = Some("USER_ID_123"))
  private val platformInfo = PlatformInfo("android", "dpixxx")
  private val Ok = "ok"

  private def runRoute(route: Route): Assertion =
    Get() ~> route ~> check(responseAs[String] shouldEqual "ok")

  "VertisEventManager" should {
    "createOfferEvent" in {
      val expectedEvent = CreateOfferEvent(Offer("OFFER_ID_123"), vertisRequestContext)
      (vertisEventSenderMock(_))
        .expects(where { actualEvent: VertisEvent.Event =>
          actualEvent.isInstanceOf[CreateOfferEvent] &&
          actualEvent.vertisRequestContext == expectedEvent.vertisRequestContext
        })
        .returning(Future.unit)
      runRoute(get {
        (pathSingleSlash & extractRequestContext) { requestContext =>
          complete {
            manager
              .createOfferEvent("123", requestContext, authInfo, platformInfo)
              .map(_ => Ok)
          }
        }
      })
    }

    "updateOfferEvent" in {
      val expectedEvent = UpdateOfferEvent(Offer("OFFER_ID_123"), vertisRequestContext)
      (vertisEventSenderMock(_))
        .expects(where { actualEvent: VertisEvent.Event =>
          actualEvent.isInstanceOf[UpdateOfferEvent] &&
          actualEvent.vertisRequestContext == expectedEvent.vertisRequestContext
        })
        .returning(Future.unit)
      runRoute(get {
        (pathSingleSlash & extractRequestContext) { requestContext =>
          complete {
            manager
              .updateOfferEvent("123", requestContext, authInfo, platformInfo)
              .map(_ => Ok)
          }
        }
      })
    }

    "deleteOfferEvent" in {
      val expectedEvent = DeleteOfferEvent(Offer("OFFER_ID_123"), vertisRequestContext)
      (vertisEventSenderMock(_))
        .expects(where { actualEvent: VertisEvent.Event =>
          actualEvent.isInstanceOf[DeleteOfferEvent] &&
          actualEvent.vertisRequestContext == expectedEvent.vertisRequestContext
        })
        .returning(Future.unit)
      runRoute(get {
        (pathSingleSlash & extractRequestContext) { requestContext =>
          complete {
            manager
              .deleteOfferEvent("123", requestContext, authInfo, platformInfo)
              .map(_ => Ok)
          }
        }
      })
    }

    "createUserEvent" in {
      val expectedEvent = CreateUserEvent(User("USER_ID_123", Seq("phone1", "phone2")), vertisRequestContext)
      (vertisEventSenderMock(_))
        .expects(where { actualEvent: VertisEvent.Event =>
          actualEvent.isInstanceOf[CreateUserEvent] &&
          actualEvent.vertisRequestContext == expectedEvent.vertisRequestContext
        })
        .returning(Future.unit)
      runRoute(get {
        (pathSingleSlash & extractRequestContext) { requestContext =>
          complete {
            manager
              .createUserEvent(Seq("phone1", "phone2"), requestContext, authInfo, platformInfo)
              .map(_ => Ok)
          }
        }
      })
    }

    "updateUserEvent" in {
      val expectedEvent = UpdateUserEvent(User("USER_ID_123", Seq("phone1", "phone2")), vertisRequestContext)
      (vertisEventSenderMock(_))
        .expects(where { actualEvent: VertisEvent.Event =>
          actualEvent.isInstanceOf[UpdateUserEvent] &&
          actualEvent.vertisRequestContext == expectedEvent.vertisRequestContext
        })
        .returning(Future.unit)
      runRoute(get {
        (pathSingleSlash & extractRequestContext) { requestContext =>
          complete {
            manager
              .updateUserEvent(Seq("phone1", "phone2"), requestContext, authInfo, platformInfo)
              .map(_ => Ok)
          }
        }
      })
    }
  }
}
