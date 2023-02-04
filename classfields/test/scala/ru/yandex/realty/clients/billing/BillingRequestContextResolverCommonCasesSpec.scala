package ru.yandex.realty.clients.billing

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.billing.BillingClient.ClientId
import ru.yandex.realty.clients.billing.gen.{BillingDomainGenerators, BillingGenerators}
import ru.yandex.realty.model.user.UserRefGenerators
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class BillingRequestContextResolverCommonCasesSpec
  extends AsyncSpecBase
  with PropertyChecks
  with UserRefGenerators
  with BillingDomainGenerators
  with BillingGenerators {

  private val billingClientMock = mock[BillingClient]

  private val resolver = new BillingRequestContextResolver(_ => billingClientMock)

  private val client: Client = Client(None, None, None, None, agency = false, None, None, 234189, None, "whyNot", None)
  private val agency: Client = Client(None, None, None, None, agency = true, None, None, 2323525, None, "indeed", None)

  private val billingClientGetUserMock =
    toMockFunction2(billingClientMock.getUser(_: String)(_: Traced))

  private val billingClientGetCustomerMock = toMockFunction4(
    billingClientMock.getCustomer(_: String, _: ClientId, _: Option[ClientId])(_: Traced)
  )

  implicit val traced: Traced = Traced.empty

  "BillingRequestContextResolver" when {
    "user with direct client, clientId:present" should {
      "fail with NoSuchElementException" in {
        forAll(billingDomainGen, passportUserGen, posNum[ClientId]) { (billingDomain, user, clientId) =>
          billingClientGetUserMock
            .expects(user.uid.toString, *)
            .returningF(Some(User("ordinary", Some(client), Seq.empty)))

          val r = resolver.resolve(billingDomain, user.uid, Some(clientId))
          r.failed.futureValue shouldBe a[NoSuchElementException]
        }
      }
    }

    "user with agency, clientId:present" should {
      "succeed with CustomerContext" in {
        forAll(billingDomainGen, passportUserGen, posNum[ClientId]) { (billingDomain, user, clientId) =>
          billingClientGetUserMock
            .expects(user.uid.toString, *)
            .returningF(Some(User("ordinary", Some(agency), Seq.empty)))

          val customer =
            Customer(clientId, client.copy(id = clientId), Some(agency.id), Some(agency), Seq.empty, Seq.empty)

          billingClientGetCustomerMock
            .expects(user.uid.toString, clientId, agency.agencyId, *)
            .returningF(Some(customer))

          val r = resolver.resolve(billingDomain, user.uid, Some(clientId))
          r.futureValue shouldBe CustomerContext(billingDomain, user.uid, customer)
        }
      }
    }

    "user with billing customer, clientId:present" should {
      "fail with NoSuchElementException" in {
        forAll(billingDomainGen, passportUserGen, posNum[ClientId]) { (billingDomain, user, clientId) =>
          val customer =
            Customer(clientId, client.copy(id = clientId), Some(agency.id), Some(agency), Seq.empty, Seq.empty)

          billingClientGetUserMock
            .expects(user.uid.toString, *)
            .returningF(Some(User("ordinary", None, Seq(customer))))

          val r = resolver.resolve(billingDomain, user.uid, Some(clientId))
          r.failed.futureValue shouldBe a[NoSuchElementException]
        }
      }
    }

    "user with direct client, clientId:missing" should {
      "succeed with UnregisteredClientContext" in {
        forAll(billingDomainGen, passportUserGen) { (billingDomain, user) =>
          billingClientGetUserMock
            .expects(user.uid.toString, *)
            .returningF(Some(User("ordinary", Some(client), Seq.empty)))

          val r = resolver.resolve(billingDomain, user.uid, None)
          r.futureValue shouldBe UnregisteredClientContext(billingDomain, user.uid, client)
        }
      }
    }

    "user with agency, clientId:missing" should {
      "succeed with AgencyContext" in {
        forAll(billingDomainGen, passportUserGen) { (billingDomain, user) =>
          billingClientGetUserMock
            .expects(user.uid.toString, *)
            .returningF(Some(User("ordinary", Some(agency), Seq.empty)))

          val r = resolver.resolve(billingDomain, user.uid, None)
          r.futureValue shouldBe AgencyContext(billingDomain, user.uid, agency)
        }
      }
    }

    "user with billing customer, clientId:missing" should {
      "succeed with CustomerContext" in {
        forAll(billingDomainGen, passportUserGen) { (billingDomain, user) =>
          val customer =
            Customer(client.id, client, Some(agency.id), Some(agency), Seq.empty, Seq.empty)

          billingClientGetUserMock
            .expects(user.uid.toString, *)
            .returningF(Some(User("ordinary", None, Seq(customer))))

          val r = resolver.resolve(billingDomain, user.uid, None)
          r.futureValue shouldBe CustomerContext(billingDomain, user.uid, customer)
        }
      }
    }
  }

}
