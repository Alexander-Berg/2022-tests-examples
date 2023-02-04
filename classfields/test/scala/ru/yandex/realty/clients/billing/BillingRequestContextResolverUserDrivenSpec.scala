package ru.yandex.realty.clients.billing

import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.billing.BillingClient.ClientId
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.billing.BillingDomains.OffersBillingDomain
import ru.yandex.realty.model.exception.BillingCustomerNotFoundException
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class BillingRequestContextResolverUserDrivenSpec extends AsyncSpecBase with RequestAware with OneInstancePerTest {

  private val billingClient = mock[BillingClient]
  private val contextBuilder = new BillingRequestContextResolver(_ => billingClient)

  val capaPartnerId = "capa_partner_id"

  val clientNotForAgency = new Client(
    city = None,
    name = None,
    email = None,
    url = None,
    agency = false,
    agencyId = None,
    regionId = None,
    id = 21,
    fax = None,
    `type` = "some type",
    phone = None
  )

  val clientForAgency = new Client(
    city = None,
    name = None,
    email = None,
    url = None,
    agency = true,
    agencyId = Some(31),
    regionId = None,
    id = 22,
    fax = None,
    `type` = "some type",
    phone = None
  )

  val customerNotForAgency = Customer(
    clientId = 11,
    client = clientNotForAgency,
    agencyId = None,
    agency = None,
    resourceRefs = Seq.empty,
    resources = Seq.empty
  )

  val customerForAgency = Customer(
    clientId = 12,
    client = clientForAgency,
    agencyId = clientForAgency.agencyId,
    agency = None,
    resourceRefs = Seq.empty,
    resources = Seq(
      Resource(
        feedUrl = None,
        host = None,
        capaPartnerId = Some(capaPartnerId),
        offlineBiz = None,
        developerId = None,
        user = None
      )
    )
  )
  val userIsNotAgency = Some(User("USER", None, Seq(customerNotForAgency)))
  val userIsNotAgencyWithoutCustomer = Some(User("USER", Some(clientNotForAgency), Seq.empty))
  val userIsAgency = Some(User("USER", Some(clientForAgency), Seq.empty))
  val existingUserIsNotAgencyUid = "123456"
  val existingUserIsAgencyUid = "912345"
  val existingUserIsNotAgencyWithoutCustomerUid = "991234"
  val notExistingUserUid = "654321"

  {
    (billingClient
      .getUser(_: String)(_: Traced))
      .expects(*, *)
      .onCall((uid: String, traced: Traced) => {
        if (uid == existingUserIsNotAgencyUid) {
          Future.successful(userIsNotAgency)
        } else if (uid == existingUserIsAgencyUid) {
          Future.successful(userIsAgency)
        } else if (uid == existingUserIsNotAgencyWithoutCustomerUid) {
          Future.successful(userIsNotAgencyWithoutCustomer)
        } else {
          Future.successful(None)
        }
      })
      .anyNumberOfTimes()
  }

  "BillingRequestContextResolver.resolve " should {

    "return error if user was not found in billing " in {
      contextBuilder
        .resolve(OffersBillingDomain, notExistingUserUid, None)
        .failed
        .futureValue shouldBe a[NoSuchElementException]
    }

    "return context if user is agency and billingClient contains Customer for clientId and agencyId" in {
      (billingClient
        .getCustomer(_: String, _: ClientId, _: Option[ClientId])(_: Traced))
        .expects(existingUserIsAgencyUid, customerForAgency.clientId, customerForAgency.agencyId, *)
        .returning(Future.successful(Some(customerForAgency)))

      val result =
        contextBuilder.resolve(OffersBillingDomain, existingUserIsAgencyUid, Some(customerForAgency.clientId))

      result.futureValue should be(CustomerContext(OffersBillingDomain, existingUserIsAgencyUid, customerForAgency))
    }

    "return error if user is agency and billingClient does not contain Customer and Client for clientId and agencyId" in {
      val wrongClientId = 0
      (billingClient
        .getCustomer(_: String, _: ClientId, _: Option[ClientId])(_: Traced))
        .expects(existingUserIsAgencyUid, wrongClientId, customerForAgency.agencyId, *)
        .returning(Future.successful(None))

      (billingClient
        .getClient(_: ClientId, _: Option[ClientId], _: String)(_: Traced))
        .expects(wrongClientId, customerForAgency.agencyId, existingUserIsAgencyUid, *)
        .returning(Future.successful(None))

      contextBuilder
        .resolve(OffersBillingDomain, existingUserIsAgencyUid, Some(wrongClientId))
        .failed
        .futureValue shouldBe a[BillingCustomerNotFoundException]
    }

    "return UnregisteredClientContext if user is agency and billingClient does not contain Customer, but contains Client" in {
      val clientId = 1234
      (billingClient
        .getCustomer(_: String, _: ClientId, _: Option[ClientId])(_: Traced))
        .expects(existingUserIsAgencyUid, clientId, customerForAgency.agencyId, *)
        .returning(Future.successful(None))

      (billingClient
        .getClient(_: ClientId, _: Option[ClientId], _: String)(_: Traced))
        .expects(clientId, customerForAgency.agencyId, existingUserIsAgencyUid, *)
        .returning(
          Future.successful(
            Some(
              ClientDescription(
                id = clientId,
                agencyId = customerForAgency.agencyId,
                client = clientNotForAgency,
                resource = None,
                isAgencyInService = false
              )
            )
          )
        )

      contextBuilder
        .resolve(OffersBillingDomain, existingUserIsAgencyUid, Some(clientId))
        .futureValue shouldBe UnregisteredClientContext(
        OffersBillingDomain,
        existingUserIsAgencyUid.toLong,
        clientNotForAgency
      )
    }

    "return agencyContext if user is agency and clientId is empty" in {
      val got = contextBuilder.resolve(OffersBillingDomain, existingUserIsAgencyUid, None).futureValue

      got should be(AgencyContext(OffersBillingDomain, existingUserIsAgencyUid.toLong, clientForAgency))
    }

    "return context with head Customer if user is not agency and contains Customer, clientId is None" in {
      val result = contextBuilder.resolve(OffersBillingDomain, existingUserIsNotAgencyUid, None)

      val expected = CustomerContext(OffersBillingDomain, existingUserIsNotAgencyUid, customerNotForAgency)
      result.futureValue should be(expected)
    }

    "return error if user is not agency and does not contain Customer, clientId is None" in {
      val expected =
        UnregisteredClientContext(
          OffersBillingDomain,
          existingUserIsNotAgencyWithoutCustomerUid.toLong,
          clientNotForAgency
        )
      contextBuilder
        .resolve(OffersBillingDomain, existingUserIsNotAgencyWithoutCustomerUid, None)
        .futureValue should be(expected)
    }

    "return error if user is not agency and contains Customer, but clientId is not None and not the same as Customer.clientId" in {
      contextBuilder
        .resolve(OffersBillingDomain, existingUserIsNotAgencyUid, Some(0))
        .failed
        .futureValue shouldBe a[NoSuchElementException]
    }
  }

}
