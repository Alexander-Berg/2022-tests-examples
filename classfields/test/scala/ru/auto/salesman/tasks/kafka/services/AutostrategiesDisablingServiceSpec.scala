package ru.auto.salesman.tasks.kafka.services

import org.joda.time.LocalDate
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ApiOfferModel.OfferStatus.ACTIVE
import ru.auto.api.ResponseModel.OfferListingResponse
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.client.VosClient.GetUserOffersQuery
import ru.auto.salesman.model.autostrategies.{AlwaysAtFirstPagePayload, Autostrategy}
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.{autostrategies, AutoruDealer}
import ru.auto.salesman.service.AutostrategiesService
import ru.auto.salesman.tasks.kafka.services.ondelete.AutostrategiesDisablingService
import ru.auto.salesman.test.{DeprecatedMockitoBaseSpec, TestException}

class AutostrategiesDisablingServiceSpec extends DeprecatedMockitoBaseSpec {

  private val autostrategiesService: AutostrategiesService =
    mock[AutostrategiesService]
  private val vosClient: VosClient = mock[VosClient]

  private val autostrategiesDisablingService =
    new AutostrategiesDisablingService(
      autostrategiesService,
      vosClient
    )

  private val offerId = "1079468494-0ba7b"
  private val offerIdentity = OfferIdentity("1079468494-0ba7b")
  private val dealerId = 1L

  "AutostrategiesDisablingService" should {

    "work successfully" in {
      when(
        vosClient.getUserOffers(
          GetUserOffersQuery(
            AutoruDealer(dealerId).toString,
            statuses = Seq(ACTIVE)
          )
        )
      )
        .thenReturnZ(
          OfferListingResponse
            .newBuilder()
            .addOffers(Offer.newBuilder().setId(offerId))
            .build()
        )
      when(autostrategiesService.get(Iterable(offerIdentity)))
        .thenReturnZ(
          Iterable(
            autostrategies.OfferAutostrategies(
              offerIdentity,
              Iterable(
                Autostrategy(
                  offerIdentity,
                  LocalDate.now().minusDays(1),
                  LocalDate.now().plusDays(1),
                  None,
                  AlwaysAtFirstPagePayload(
                    forMarkModelListing = false,
                    forMarkModelGenerationListing = false
                  )
                )
              )
            )
          )
        )
      when(autostrategiesService.delete(?)).thenReturnZ(())

      autostrategiesDisablingService
        .disableAutostrategies(dealerId)
        .success
    }

    "fail if unable to receive user offers" in {
      when(
        vosClient.getUserOffers(
          GetUserOffersQuery(
            AutoruDealer(dealerId).toString,
            statuses = Seq(ACTIVE)
          )
        )
      )
        .thenThrowZ(new TestException)

      autostrategiesDisablingService
        .disableAutostrategies(1)
        .failure
        .exception shouldBe a[TestException]
    }

    "fail if unable to receive autostrategies" in {
      when(
        vosClient.getUserOffers(
          GetUserOffersQuery(
            AutoruDealer(dealerId).toString,
            statuses = Seq(ACTIVE)
          )
        )
      )
        .thenReturnZ(
          OfferListingResponse
            .newBuilder()
            .addOffers(Offer.newBuilder().setId(offerId))
            .build()
        )
      when(autostrategiesService.get(?)).thenThrowZ(new TestException())

      autostrategiesDisablingService
        .disableAutostrategies(1)
        .failure
        .exception shouldBe a[TestException]
    }

    "fail if unable to delete autostrategies" in {
      when(
        vosClient.getUserOffers(
          GetUserOffersQuery(
            AutoruDealer(dealerId).toString,
            statuses = Seq(ACTIVE)
          )
        )
      )
        .thenReturnZ(
          OfferListingResponse
            .newBuilder()
            .addOffers(Offer.newBuilder().setId(offerId))
            .build()
        )
      when(autostrategiesService.get(Iterable(offerIdentity)))
        .thenReturnZ(
          Iterable(
            autostrategies.OfferAutostrategies(
              offerIdentity,
              Iterable(
                Autostrategy(
                  offerIdentity,
                  LocalDate.now().minusDays(1),
                  LocalDate.now().plusDays(1),
                  None,
                  AlwaysAtFirstPagePayload(
                    forMarkModelListing = false,
                    forMarkModelGenerationListing = false
                  )
                )
              )
            )
          )
        )
      when(autostrategiesService.delete(?)).thenThrowZ(new TestException())

      autostrategiesDisablingService
        .disableAutostrategies(1)
        .failure
        .exception shouldBe a[TestException]
    }
  }

}
