package ru.auto.salesman.tasks.processors.impl

import ru.auto.cabinet.DealerAutoru
import ru.auto.cabinet.DealerAutoru.Dealer
import ru.auto.salesman.service.DealerFeatureService
import ru.auto.salesman.tasks.kafka.processors.impl.{
  ActiveDealerUpdatesProcessorImpl,
  DealerUpdateProcessorImpl,
  DeletedDealerUpdatesProcessorImpl,
  NewDealerUpdatesProcessorImpl
}
import ru.auto.salesman.test.BaseSpec

class DealerUpdateProcessorImplSpec extends BaseSpec {

  val activateDealerUpdateProcessor = mock[ActiveDealerUpdatesProcessorImpl]
  val deactivateDealerUpdateProcessor = mock[DeletedDealerUpdatesProcessorImpl]
  val newDealerUpdatesProcessor = mock[NewDealerUpdatesProcessorImpl]
  val featureService = mock[DealerFeatureService]

  val service = new DealerUpdateProcessorImpl(
    activateDealerUpdateProcessor,
    deactivateDealerUpdateProcessor,
    newDealerUpdatesProcessor,
    featureService
  )

  "DealerUpdateProcessorImpl" should {
    "activate dealer if request status activation and dealer is not agent" in {
      val dealer = testDealerBuilder
        .setStatus(Dealer.Status.ACTIVE)
        .setIsAgent(false)
        .build()
      mockEmptySkipErrorChangeStatusDealerIds()
      (activateDealerUpdateProcessor.process _).expects(dealer).returningZ(Unit)
      service.process(dealer).success
    }

    "deactivate dealer if request status DELETED and dealer is not agent" in {
      val dealer = testDealerBuilder
        .setStatus(Dealer.Status.DELETED)
        .setIsAgent(false)
        .build()
      mockEmptySkipErrorChangeStatusDealerIds()
      (deactivateDealerUpdateProcessor.process _)
        .expects(dealer)
        .returningZ(Unit)

      service.process(dealer).success
    }

    "skip dealer if request status activation and dealer is agent" in {
      val dealer = testDealerBuilder
        .setStatus(Dealer.Status.ACTIVE)
        .setIsAgent(true)
        .build()
      mockEmptySkipErrorChangeStatusDealerIds()
      service.process(dealer).success
    }

    "deactivate dealer if request status DELETED and dealer is agent" in {

      val dealer = testDealerBuilder
        .setStatus(Dealer.Status.DELETED)
        .setIsAgent(true)
        .build()
      mockEmptySkipErrorChangeStatusDealerIds()
      service.process(dealer).success
    }

    "process dealer with NewDealerUpdatesProcessorImpl if status is New and dealer is not and agent" in {
      val dealer = testDealerBuilder
        .setStatus(Dealer.Status.NEW)
        .setIsAgent(false)
        .build()
      mockEmptySkipErrorChangeStatusDealerIds()
      (newDealerUpdatesProcessor.process _).expects(dealer).returningZ(Unit)
      service.process(dealer).success
    }

    "skip dealer if request status is not DELETED or ACTIVE" in {
      val dealer = testDealerBuilder
        .setStatus(Dealer.Status.WAITING)
        .setIsAgent(true)
        .build()
      mockEmptySkipErrorChangeStatusDealerIds()
      service.process(dealer).success
    }

    "skip dealer if dealer status DELETED and dealer id contains in feature" in {
      val dealer = testDealerBuilder
        .setStatus(Dealer.Status.DELETED)
        .setIsAgent(false)
        .build()
      (featureService.skipErrorChangeStatusDealerIds _)
        .expects()
        .returning(Set(112L))
      service.process(dealer).success
    }
  }

  private def mockEmptySkipErrorChangeStatusDealerIds(): Unit =
    (featureService.skipErrorChangeStatusDealerIds _)
      .expects()
      .returning(Set.empty)

  val testDealerBuilder = DealerAutoru.Dealer
    .newBuilder()
    .setId(112)
    .setAddress("test addtess")

}
