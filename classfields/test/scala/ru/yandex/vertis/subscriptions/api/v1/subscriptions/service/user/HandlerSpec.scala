package ru.yandex.vertis.subscriptions.api.v1.subscriptions.service.user

import ru.yandex.vertis.subscriptions.Model._
import ru.yandex.vertis.subscriptions.api.util.Protobuf
import ru.yandex.vertis.subscriptions.api.{DomainMarshallers, RouteTestWithConfig}
import ru.yandex.vertis.subscriptions.backend.actor.MailConfirmationRequester
import ru.yandex.vertis.subscriptions.backend.confirmation.impl._
import ru.yandex.vertis.subscriptions.backend.user.impl.{UserServiceImpl, UserSubscriptionsServiceImpl}
import ru.yandex.vertis.subscriptions.backend.user.logging.LoggingUserService
import ru.yandex.vertis.subscriptions.core.plugin.TrivialRequestParser
import ru.yandex.vertis.subscriptions.service.impl.JvmDraftService
import ru.yandex.vertis.subscriptions.storage.memory.{InMemoryUserLinks, SubscriptionsDao}
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.subscriptions.{SpecBase, TestExecutionContext}

import akka.testkit.{TestActorRef, TestProbe}
import com.googlecode.protobuf.format.JsonFormat
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import spray.http._
import spray.routing.HttpService

import java.net.URLEncoder

import scala.concurrent.duration._

/** Specs on [[ru.yandex.vertis.subscriptions.api.v1.subscriptions.service.user.Handler]]
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends SpecBase with RouteTestWithConfig with HttpService with TestExecutionContext {

  val subscriptionConfirmationRequester = TestProbe()
  val mailConfirmationRequester = TestProbe()

  val userSubscriptions = new MemoryUserProperties
  val userMails = new MemoryUserProperties

  val subscriptionConfirmation = new SubscriptionConfirmationService(userSubscriptions)
    with EmitSubscriptionsConfirmationViaActor {
    val confirmationRequester = subscriptionConfirmationRequester.ref
  }

  val emailConfirmation = new MailConfirmationService(userMails) with EmitMailConfirmation {
    val confirmationRequester = mailConfirmationRequester.ref
  }

  val dao = new SubscriptionsDao

  val userLinks = new InMemoryUserLinks

  val userSubscriptionsService =
    new UserSubscriptionsServiceImpl(
      dao,
      userLinks,
      TrivialRequestParser,
      "auto",
      sandboxMode = true,
      lightWeightEc = ec,
      blockingEc = ec
    )

  val drafts = new JvmDraftService

  val userService = new UserServiceImpl(
    userSubscriptionsService,
    subscriptionConfirmation,
    emailConfirmation,
    drafts,
    ec,
    ec
  ) with LoggingUserService

  val invalidJsonSubscription =
    """{
    "delivery": {
      "email": {
      "address": "test@.ru",
      "period": 0
    }
    },
    "request": {
      "http_query": "test=test"
    },
    "view": {
      "title": "test",
      "body": "test",
      "language": "ru",
      "currency": "RUR",
      "top_level_domain": "ru",
      "domain": "auto"
    },
    "internal_settings": {
      "send_email_in_testing": true
    }
  }"""

  /** Route of handler under test */
  private val route = seal {
    TestActorRef(new Handler(userService)).underlyingActor.route
  }

  "GET /uid/1" should {
    "return subscriptions from user identified by passport uid 1" in {
      Get("/uid/1") ~> route ~> check {
        responseAs[String] should be("[]")
      }
    }
  }

  "GET /billing/_" should {
    "return subscriptions from user identified by pair billing id" in {
      Get("/billing/auto/agency_id:10;client_id:15") ~> route ~> check {
        responseAs[String] should be("[]")
      }
    }

    "return subscriptions from user identified by agency id" in {
      Get("/billing/tours/agency_id:10") ~> route ~> check {
        responseAs[String] should be("[]")
      }
    }

    "return subscriptions from user identified by client id" in {
      Get("/billing/auto/client_id:15") ~> route ~> check {
        responseAs[String] should be("[]")
      }
    }
  }

  "GET /autoru/*" should {
    "return subscriptions from Auto.ru authenticated user" in {
      Get("/autoru/uid:42") ~> route ~> check {
        responseAs[String] should be("[]")
      }
    }

    "return subscriptions from Auto.ru non-authenticated user" in {
      Get("/autoru/sid:100500") ~> route ~> check {
        responseAs[String] should be("[]")
      }
    }

    "not return subscriptions from unknown auto.ru identity type" in {
      Get("/autoru/foo:baz") ~> sealRoute(route) ~> check {

        status should be(StatusCodes.NotFound)
      }
    }
  }

  "GET /facebook/id123" should {
    "return 'Unknown identity type'" in {
      Get("/facebook/id123") ~> sealRoute(route) ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
  }

  "POST /uid/1 with protobuf request" should {
    val source = CoreGenerators.emailSubscriptionsSources.next
    "create subscription" in {
      Post("/uid/1", HttpEntity(Protobuf.contentType, source.toByteArray))
        .withHeaders(HttpHeaders.Accept(MediaRange(Protobuf.mediaType))) ~>
        sealRoute(route) ~>
        check {
          status should be(StatusCodes.OK)
          contentType.mediaType should be(Protobuf.mediaType)
          checkResponseWithSource(source)
        }
      Get("/uid/1").withHeaders(HttpHeaders.Accept(MediaRange(MediaTypes.`application/json`))) ~> sealRoute(route) ~> check {
        status should be(StatusCodes.OK)
        contentType should be(ContentTypes.`application/json`)
        checkResponseWithSource(source)
      }
      Get("/uid/1").withHeaders(HttpHeaders.Accept(MediaRange(Protobuf.mediaType))) ~> sealRoute(route) ~> check {
        status should be(StatusCodes.OK)
        contentType should be(Protobuf.contentType)
        checkResponseWithSource(source)
      }
      Post("/uid/1", HttpEntity(Protobuf.contentType, source.toByteArray)) ~>
        sealRoute(route) ~>
        check {
          status should be(StatusCodes.Conflict)
          checkResponseWithSource(source)
        }
    }
  }

  "POST /uid/2 with JSON request" should {
    val source = CoreGenerators.emailSubscriptionsSources.next
    "create subscription" in {
      Post("/uid/2", HttpEntity(ContentTypes.`application/json`, JsonFormat.printToString(source))) ~>
        sealRoute(route) ~>
        check {
          status should be(StatusCodes.OK)
          checkResponseWithSource(source)
        }
      Get("/uid/2").withHeaders(HttpHeaders.Accept(MediaRange(MediaTypes.`application/json`))) ~> route ~> check {
        status should be(StatusCodes.OK)
        contentType should be(ContentTypes.`application/json`)
        checkResponseWithSource(source)
      }
    }
  }

  "Search subscription" should {
    val source = CoreGenerators.emailSubscriptionsSources.next
    val query = URLEncoder.encode(source.getRequestSource.getHttpQuery, "UTF-8")
    val uid = 3

    Post(s"/uid/$uid", HttpEntity(ContentTypes.`application/json`, JsonFormat.printToString(source))) ~>
      sealRoute(route) ~>
      check {
        status should be(StatusCodes.OK)
        assert(responseAs[String].contains(source.getDelivery.getEmail.getAddress))
      }
    "find created subscription by query" in {
      Get(s"/uid/$uid?query=$query").withHeaders(HttpHeaders.Accept(MediaRange(MediaTypes.`application/json`))) ~>
        sealRoute(route) ~>
        check {
          status should be(StatusCodes.OK)
          contentType should be(ContentTypes.`application/json`)
          assert(responseAs[String].contains(source.getDelivery.getEmail.getAddress))
        }
    }
    "not find created subscription by foreign query" in {
      Get(s"/uid/$uid/?query=${query.dropRight(1)}")
        .withHeaders(HttpHeaders.Accept(MediaRange(MediaTypes.`application/json`))) ~>
        sealRoute(route) ~>
        check {
          status should be(StatusCodes.OK)
          responseAs[String] should be("[]")
        }
    }
    "respond with 400 BadRequest for request with incorrect params" in {
      Get(s"/uid/$uid/find/?quer_y=${query.dropRight(1)}")
        .withHeaders(HttpHeaders.Accept(MediaRange(MediaTypes.`application/json`))) ~>
        sealRoute(route) ~>
        check {
          status should be(StatusCodes.NotFound)
        }
    }
  }

  "GET /uid/3/id" should {
    "return NotFound" in {
      Get("/uid/3/id") ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
  }

  "Deal with email confirmation" should {
    val user = User.newBuilder().setUid("1").build()
    val email = "email@example.com"
    "produce email confirmation request" in {
      Get(s"/uid/1/request-confirmation?email=$email") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
      val property = userMails.getProperty(user, email).get
      property should be(defined)
    }

    "not confirm with invalid token" in {
      Get(s"/uid/1/confirm?email=$email&token=foo") ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
      }
    }

    "confirm with correct token" in {
      val property = userMails.getProperty(user, email).get
      val token = property.get.getConfirmationHash
      Get(s"/uid/1/confirm?email=$email&token=$token") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  "Deal with subscription confirmation" should {
    val source = CoreGenerators.emailSubscriptionsSources.next

    val user = User.newBuilder().setUid("100").build()
    var id: String = null

    "create subscription and produce email confirmation request" in {
      Post(s"/uid/${user.getUid}", HttpEntity(ContentTypes.`application/json`, JsonFormat.printToString(source))) ~>
        sealRoute(route) ~>
        check {
          status should be(StatusCodes.OK)
          assert(responseAs[String].contains(source.getDelivery.getEmail.getAddress))
        }

      mailConfirmationRequester.fishForMessage() {
        case MailConfirmationRequester.Request(`user`, _, _) => true
        case MailConfirmationRequester.Request(_, _, _) => false
      }

      val subs = userService.list(user).futureValue
      subs should have size (1)
      id = subs.head.getId
    }

    "create subscription with predefined state" in {
      val nextSubscription = CoreGenerators.emailSubscriptionsSources.next
      val outerSource = OuterSubscriptionSource
        .newBuilder(DomainMarshallers.asOuterSubscriptionSource(nextSubscription))
        .setState(State.newBuilder().setValue(State.Value.ACTIVE).setTimestamp(System.currentTimeMillis))
        .build

      Post(s"/uid/${user.getUid}", HttpEntity(ContentTypes.`application/json`, JsonFormat.printToString(outerSource))) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          assert(responseAs[String].contains(outerSource.getDelivery.getEmail.getAddress))
        }
      mailConfirmationRequester.expectNoMessage(500.millis)
    }

//    "produce badRequest in case of invalid SubscriptionSource" in {
//      Post(s"/uid/${user.getUid}", HttpEntity(ContentTypes.`application/json`, JsonFormat.printToString(invalidSource))) ~>
//        sealRoute(route) ~>
//        check {
//          status should be(StatusCodes.BadRequest)
//        }
//    }

    "produce badRequest in case of invalid OuterSubscriptionSource" in {
      Post(s"/uid/${user.getUid}", HttpEntity(ContentTypes.`application/json`, invalidJsonSubscription)) ~>
        sealRoute(route) ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }

    "produce subscription confirmation request" in {
      Get(s"/uid/${user.getUid}/request-confirmation?id=$id") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
      val property = userSubscriptions.getProperty(user, id).get
      property should be(defined)
    }

    "not confirm with invalid token" in {
      Get(s"/uid/${user.getUid}/confirm?id=$id&token=foo") ~> route ~> check {
        status should be(StatusCodes.Unauthorized)
      }
    }

    "confirm with correct token" in {
      val property = userSubscriptions.getProperty(user, id).get
      val token = property.get.getConfirmationHash
      Get(s"/uid/${user.getUid}/confirm?id=$id&token=$token") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  "Deal with different user types" should {
    "with domain yandex user" in {
      Get(s"/yandex:realty:uid:1") ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  private def checkResponseWithSource(source: SubscriptionSource) {
    val resp = responseAs[String]
    assert(resp.contains(source.getRequestSource.getHttpQuery))
    if (source.getDelivery.hasEmail)
      assert(resp.contains(source.getDelivery.getEmail.getAddress))
  }

  implicit def actorRefFactory = system
}
