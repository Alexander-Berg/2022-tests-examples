package ru.yandex.realty.api.directives

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.RouteDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.akka.http.ProtobufSupport
import ru.yandex.realty.akka.http.headers.RequestHeader
import ru.yandex.realty.clients.billing.BillingClient.ClientId
import ru.yandex.realty.clients.billing.gen.BillingGenerators
import ru.yandex.realty.clients.billing.{AgencyContext, BillingRequestContextResolver, UnregisteredUserContext}
import ru.yandex.realty.model.billing.{BillingDomain, BillingDomains}
import ru.yandex.realty.model.user.{PassportUser, UserRef}
import ru.yandex.realty.request.TestRequest
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class BillingDirectivesSpec
  extends SpecBase
  with ScalatestRouteTest
  with PropertyChecks
  with BillingGenerators
  with RouteDirectives
  with ProtobufSupport {

  override def testConfig: Config = ConfigFactory.empty()

  private val resolver = mock[BillingRequestContextResolver]

  private val directive = new BillingDirectives {
    override def billingRequestContextResolver: BillingRequestContextResolver = resolver

    val mockResolve = toMockFunction4(
      billingRequestContextResolver.resolve(_: BillingDomain, _: Long, _: Option[ClientId])(_: Traced)
    )
  }

  private val request = TestRequest.fromPassport(12341234L)

  "WithBillingDomain" when {
    val requestBuilder: String => HttpRequest = path => Get(s"/$path").addHeader(RequestHeader(request))

    "offers context requested" should {
      "succeed" in {
        requestBuilder("offers") ~> directive.WithBillingDomain(v => complete(v.toString)) ~> check {
          responseAs[String] shouldEqual BillingDomains.OffersBillingDomain.toString
        }
      }
    }

    "calls context requested" should {
      "succeed" in {
        requestBuilder("calls") ~> directive.WithBillingDomain(v => complete(v.toString)) ~> check {
          responseAs[String] shouldEqual BillingDomains.CallsBillingDomain.toString
        }
      }
    }

    "wrong context requested" should {
      "reject" in {
        requestBuilder("WRONG") ~> directive.WithBillingDomain(v => complete(v.toString)) ~> check {
          handled shouldEqual false
          rejection shouldBe ValidationRejection("Unsupported billing domain [WRONG]")
        }
      }
    }
  }

  "WithBillingContext" when {
    val requestBuilder: (String, UserRef, Option[String]) => HttpRequest = { (billingDomain, userRef, clientId) =>
      val basicUri = Uri(s"/$billingDomain/user/${userRef.toPlain}")
      val uri = clientId
        .map(clientIdValue => basicUri.withQuery(Uri.Query("client" -> clientIdValue)))
        .getOrElse(basicUri)
      Get(uri).addHeader(RequestHeader(request))
    }

    "valid billingDomain and Passport user" should {
      "conclude to UnregisteredUserContext" in {
        forAll(billingDomainGen, passportUserGen) { (billingDomain, userRef) =>
          val expected = UnregisteredUserContext(billingDomain, userRef.uid)
          directive.mockResolve
            .expects(billingDomain, userRef.uid, None, *)
            .returningF(expected)

          requestBuilder(billingDomain.toString, userRef, None) ~>
            directive.WithBillingContext { v =>
              v shouldEqual expected
              complete("")
            }
        }
      }

      "conclude to AgencyContext" in {
        forAll(billingDomainGen, passportUserGen, agencyGen) { (billingDomain, userRef, agencyClient) =>
          val expected = AgencyContext(billingDomain, userRef.uid, agencyClient)
          directive.mockResolve
            .expects(billingDomain, userRef.uid, None, *)
            .returningF(expected)

          requestBuilder(billingDomain.toString, userRef, None) ~>
            directive.WithBillingContext { v =>
              v shouldEqual expected
              complete("")
            }
        }
      }

      "conclude to CustomerContext for user (with agency) + clientId" in {
        forAll(customerContextGen, agencyClientGen) { (customerContext, agencyClient) =>
          val expected = customerContext
          val billingDomain = customerContext.billingDomain
          val uid = customerContext.uid
          directive.mockResolve
            .expects(billingDomain, uid, Some(agencyClient.id), *)
            .returningF(expected)

          requestBuilder(billingDomain.toString, PassportUser(uid), Some(agencyClient.id.toString)) ~>
            directive.WithBillingContext { v =>
              v shouldEqual expected
              complete("")
            }
        }
      }
    }
  }

  "WithClientType" when {
    val requestBuilder: Option[String] => HttpRequest = { queryType =>
      val basicUri = Uri("/")
      val uri = queryType
        .map(queryTypeValue => basicUri.withQuery(Uri.Query("queryType" -> queryTypeValue)))
        .getOrElse(basicUri)
      Get(uri).addHeader(RequestHeader(request))
    }

    "agencies client type" should {
      "succeed" in {
        requestBuilder(Some("agencies")) ~> directive.WithClientType({ v =>
          complete(v.asBillingClientQueryType)
        }) ~> check {
          responseAs[String] shouldEqual "Agencies"
        }
      }
    }

    "clients type" should {
      "succeed" in {
        requestBuilder(Some("clients")) ~> directive.WithClientType(v => complete(v.asBillingClientQueryType)) ~> check {
          responseAs[String] shouldEqual "DirectClients"
        }
      }
    }

    "all type" should {
      "succeed" in {
        requestBuilder(Some("all")) ~> directive.WithClientType(v => complete(v.asBillingClientQueryType)) ~> check {
          responseAs[String] shouldEqual "AllWithoutSubClients"
        }
      }
    }

    "no type" should {
      "succeed" in {
        requestBuilder(None) ~> directive.WithClientType(v => complete(v.asBillingClientQueryType)) ~> check {
          responseAs[String] shouldEqual "AllWithoutSubClients"
        }
      }
    }

    "WRONG type" should {
      "reject" in {
        requestBuilder(Some("WRONG")) ~> directive.WithClientType(v => complete(v.asBillingClientQueryType)) ~> check {
          handled shouldEqual false
          rejection shouldBe MalformedQueryParamRejection("queryType", "Unsupported client queryType: [WRONG]", None)
        }
      }
    }
  }

}
