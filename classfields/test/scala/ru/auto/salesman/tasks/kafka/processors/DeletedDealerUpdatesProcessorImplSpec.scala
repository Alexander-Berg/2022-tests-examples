package ru.auto.salesman.tasks.kafka.processors

import ru.auto.api.ApiOfferModel.Offer
import ru.auto.cabinet.DealerAutoru.Dealer
import ru.auto.salesman.client.exceptions.CustomerNotFoundException
import ru.auto.salesman.dao.user.ProductScheduleDao
import ru.auto.salesman.dao.{QuotaDao, QuotaRequestDao}
import ru.auto.salesman.exceptions.CompositeException
import ru.auto.salesman.service.AdsRequestService
import ru.auto.salesman.tasks.kafka.processors.impl.DeletedDealerUpdatesProcessorImpl
import ru.auto.salesman.tasks.kafka.services.BillingTestData
import ru.auto.salesman.tasks.kafka.services.ondelete.{
  AutostrategiesDisablingService,
  BillingCampaignDisablingService
}
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.{DeprecatedMockitoBaseSpec, TestException}

import java.util.NoSuchElementException
import scala.util.{Failure, Success, Try}

class DeletedDealerUpdatesProcessorImplSpec
    extends DeprecatedMockitoBaseSpec
    with OfferModelGenerators {

  private val adsRequestService: AdsRequestService = mock[AdsRequestService]
  private val quotaRequestDao: QuotaRequestDao = mock[QuotaRequestDao]
  private val quotaDao: QuotaDao = mock[QuotaDao]
  private val productScheduleDao: ProductScheduleDao = mock[ProductScheduleDao]

  private val billingCampaignDisablingService: BillingCampaignDisablingService =
    mock[BillingCampaignDisablingService]

  private val autostrategiesDisablingService: AutostrategiesDisablingService =
    mock[AutostrategiesDisablingService]

  private val deletedDealerUpdatesProcessor: DeletedDealerUpdatesProcessorImpl =
    new DeletedDealerUpdatesProcessorImpl(
      adsRequestService,
      quotaRequestDao,
      quotaDao,
      productScheduleDao,
      autostrategiesDisablingService,
      billingCampaignDisablingService
    )

  private val testDealer = Dealer.newBuilder().setId(1).build()
  private val testDealerId = testDealer.getId

  private val testException = new TestException("test reason")

  "DeletedDealerUpdatesProcessor.process()" should {
    "should work successfully" in {
      forAll(offerGen()) { offer =>
        mockEverythingToSuccess(offer)

        deletedDealerUpdatesProcessor
          .process(testDealer)
          .success
      }
    }

    "should fail when AdsRequests disabling fails" in {
      forAll(offerGen()) { offer =>
        mockEverythingToSuccess(offer)
        mockTryProducerToFailure(
          adsRequestService.deleteAllForClient(testDealer.getId)
        )

        deletedDealerUpdatesProcessor
          .process(testDealer)
          .failure
          .exception shouldBe CompositeException(testException)
      }
    }

    "should fail when Autostrategies disabling fails" in {
      forAll(offerGen()) { offer =>
        mockEverythingToSuccess(offer)
        when(autostrategiesDisablingService.disableAutostrategies(testDealerId))
          .thenThrowZ(testException)

        deletedDealerUpdatesProcessor
          .process(testDealer)
          .failure
          .exception shouldBe CompositeException(testException)
      }

    }

    "should fail when ProductSchedules disabling fails" in {
      forAll(offerGen()) { offer =>
        mockEverythingToSuccess(offer)
        mockTryProducerToFailure(productScheduleDao.update(?, ?))

        deletedDealerUpdatesProcessor
          .process(testDealer)
          .failure
          .exception shouldBe CompositeException(testException)
      }
    }

    "should fail when BillingCampaigns disabling fails" in {
      forAll(offerGen()) { offer =>
        mockEverythingToSuccess(offer)
        when(
          billingCampaignDisablingService.disableBillingCampaigns(
            eq(testDealerId)
          )
        )
          .thenThrowZ(testException)

        deletedDealerUpdatesProcessor
          .process(testDealer)
          .failure
          .exception shouldBe CompositeException(testException)
      }
    }

    "should work successfully when BillingCampaigns disabling fails with CustomerNotFoundException" in {
      forAll(offerGen()) { offer =>
        mockEverythingToSuccess(offer)
        when(
          billingCampaignDisablingService.disableBillingCampaigns(
            eq(testDealerId)
          )
        )
          .thenThrowZ(
            CustomerNotFoundException(CustomerNotFoundException.ResponseCode)
          )

        deletedDealerUpdatesProcessor
          .process(testDealer)
          .success
      }
    }

    "should work successfully when BillingCampaigns disabling fails with NoSuchElementException" in {
      forAll(offerGen()) { offer =>
        mockEverythingToSuccess(offer)
        when(
          billingCampaignDisablingService.disableBillingCampaigns(
            eq(testDealerId)
          )
        )
          .thenThrowZ(
            new NoSuchElementException(s"Balance core info is not found: $testDealerId")
          )

        deletedDealerUpdatesProcessor
          .process(testDealer)
          .success
      }
    }

    "should fail when QuotaRequests disabling fails" in {
      forAll(offerGen()) { offer =>
        mockEverythingToSuccess(offer)
        mockTryProducerToFailure(quotaRequestDao.disableForClient(testDealerId))

        deletedDealerUpdatesProcessor
          .process(testDealer)
          .failure
          .exception shouldBe CompositeException(testException)
      }
    }

    "should fail when Quotas archiving fails" in {
      forAll(offerGen()) { offer =>
        mockEverythingToSuccess(offer)
        mockTryProducerToFailure(quotaDao.archive(?))

        deletedDealerUpdatesProcessor
          .process(testDealer)
          .failure
          .exception shouldBe CompositeException(testException)
      }
    }
  }

  def mockEverythingToSuccess(offer: Offer): Unit = {
    //AdsRequests
    when(adsRequestService.deleteAllForClient(testDealerId))
      .thenReturn(Success(()))

    //Autostrategies
    when(autostrategiesDisablingService.disableAutostrategies(testDealerId))
      .thenReturnZ(())

    //AutoApplySettings
    when(productScheduleDao.update(?, ?))
      .thenReturn(Success(()))

    //BillingCampaigns
    when(
      billingCampaignDisablingService
        .disableBillingCampaigns(eq(testDealerId))
    )
      .thenReturnZ(Iterable(BillingTestData.campaignHeader(false)))

    //QuotaRequests
    when(quotaRequestDao.disableForClient(testDealerId))
      .thenReturn(Success(()))

    //Archive Quotas
    when(quotaDao.archive(?))
      .thenReturn(Success(()))
  }

  private def mockTryProducerToFailure(f: => Try[Any]): Unit =
    when(f).thenReturn(Failure(testException))
}
