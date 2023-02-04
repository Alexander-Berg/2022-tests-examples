package ru.auto.salesman.client.retrying

import java.net.SocketTimeoutException
import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.{
  ApiOfferModel,
  OffersByParamsFilterOuterClass,
  RequestModel,
  ResponseModel
}
import ru.auto.salesman.Task
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.client.VosClient.{
  AddServicesResult,
  CountUserOffersQuery,
  GetUserOffersQuery
}
import ru.auto.salesman.client.retrying.RetryingVosClientSpec.DelegatingVosClient
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.{DbInstance, OfferTag, UserRef}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.addServicesResultGen
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.RequestContext
import zio.Ref

import scala.util.Try

class RetryingVosClientSpec extends BaseSpec with OfferModelGenerators {

  val delegate = mock[VosClient]

  val client = new DelegatingVosClient(delegate) with RetryingVosClient

  private val addServicesMock = toMockFunction4 {
    delegate.addServices
  }

  "RetryingVosClient" when {
    "addServices" should {
      "pass success" in {
        forAll(
          OfferIdentityGen,
          Gen.option(OfferCategoryGen),
          userRefGen,
          addServicesRequestGen,
          addServicesResultGen
        ) { (offerId, category, user, request, result) =>
          addServicesMock
            .expects(offerId, category, user, request)
            .returningZ(result)
            .once()

          client
            .addServices(offerId, category, user, request)
            .success
            .value shouldBe result
        }
      }

      "retry on SocketTimeoutException" in {
        forAll(
          OfferIdentityGen,
          Gen.option(OfferCategoryGen),
          userRefGen,
          addServicesRequestGen
        ) { (offerId, category, user, request) =>
          val exception = new SocketTimeoutException("artificial")
          val retryCount = Ref.make[Int](0).success.value
          addServicesMock
            .expects(offerId, category, user, request)
            .returning(retryCount.update(_ + 1) *> Task.fail(exception))

          client
            .addServices(offerId, category, user, request)
            .failure
            .exception shouldBe exception
          retryCount.get.success.value shouldBe RetryingVosClient.AddServicesRetryCount + 1
        }
      }

      "pass other fails" in {
        forAll(
          OfferIdentityGen,
          Gen.option(OfferCategoryGen),
          userRefGen,
          addServicesRequestGen
        ) { (offerId, category, user, request) =>
          val exception = new RuntimeException("artificial")
          addServicesMock
            .expects(offerId, category, user, request)
            .throwingZ(exception)
            .once()

          client
            .addServices(offerId, category, user, request)
            .failure
            .exception shouldBe exception
        }
      }
    }
  }
}

object RetryingVosClientSpec {

  class DelegatingVosClient(delegate: VosClient) extends VosClient {

    def getOptOffer(
        offerId: OfferIdentity,
        dbInstance: DbInstance
    ): Task[Option[ApiOfferModel.Offer]] =
      delegate.getOptOffer(offerId, dbInstance)

    def hasSameOffer(
        category: ApiOfferModel.Category,
        offerId: OfferIdentity,
        userRef: String
    ): Task[Boolean] =
      delegate.hasSameOffer(category, offerId, userRef)

    def hasSameOfferAsDraft(
        category: Category,
        offerId: OfferIdentity,
        userRef: String
    ): Task[Boolean] =
      delegate.hasSameOfferAsDraft(category, offerId, userRef)

    def countUserOffers(query: CountUserOffersQuery): Task[Int] =
      delegate.countUserOffers(query)

    def getUserOffers(
        query: GetUserOffersQuery
    ): Task[ResponseModel.OfferListingResponse] =
      delegate.getUserOffers(query)

    def addServices(
        offerId: OfferIdentity,
        offerCategory: Option[ApiOfferModel.Category],
        userRef: String,
        services: RequestModel.AddServicesRequest
    ): Task[AddServicesResult] =
      delegate.addServices(offerId, offerCategory, userRef, services)

    def putTags(
        offerId: OfferIdentity,
        offerCategory: Option[ApiOfferModel.Category],
        tags: Set[OfferTag]
    ): Task[Unit] =
      delegate.putTags(offerId, offerCategory, tags)

    def deleteTags(
        offerId: OfferIdentity,
        offerCategory: Option[ApiOfferModel.Category],
        tags: Set[OfferTag]
    )(implicit rc: RequestContext): Try[Unit] =
      delegate.deleteTags(offerId, offerCategory, tags)

    def getMarkModels(
        query: VosClient.GetMarkModelsQuery
    ): Task[Option[ResponseModel.MarkModelsResponse]] =
      delegate.getMarkModels(query)

    def setCountersStartDate(
        offerId: OfferIdentity,
        offerCategory: Option[ApiOfferModel.Category],
        date: Option[DateTime]
    )(implicit rc: RequestContext): Try[Unit] =
      delegate.setCountersStartDate(offerId, offerCategory, date)

    def getByParams(
        filter: OffersByParamsFilterOuterClass.OffersByParamsFilter
    ): Task[List[ApiOfferModel.Offer]] =
      delegate.getByParams(filter)

    override def checkOffersBelong(
        userRef: UserRef,
        offerIds: Seq[OfferIdentity]
    ): Task[Boolean] = delegate.checkOffersBelong(userRef, offerIds)
  }

}
