package ru.yandex.vertis.billing.banker.service.refund

import org.scalacheck.ShrinkLowPriority
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.model.State.StateStatuses
import ru.yandex.vertis.billing.banker.model.gens.{Producer, StateParams}
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds
import ru.yandex.vertis.billing.banker.service.refund.processor.DefaultRefundProcessor
import ru.yandex.vertis.billing.banker.service.refund.processor.RefundProcessingGens.{onSource, processorActionGen}
import ru.yandex.vertis.billing.banker.service.refund.processor.RefundProcessor.RefundProcessingException

class DefaultRefundProcessorSpec
  extends MockFactory
  with Matchers
  with AnyWordSpecLike
  with AsyncSpecBase
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  private val processor = DefaultRefundProcessor(PaymentSystemIds.Robokassa)

  private val actionGen = processorActionGen(processor)

  "DefaultRefundProcessor" should {
    "process/sync" when {
      "all data is correct" in {
        val stateParams = StateParams(
          stateStatus = Set(StateStatuses.Valid)
        )
        onSource(stateParams) { source =>
          val action = actionGen.next
          val refund = action(source).futureValue
          refund.amount shouldBe source.refundAmount
          refund.stateStatus shouldBe StateStatuses.Valid: Unit
        }
      }
    }
    "fail process/sync" when {
      "on partly refunded payment" in {
        val stateParams = StateParams(
          stateStatus = Set(StateStatuses.PartlyRefunded)
        )
        onSource(stateParams) { source =>
          val action = actionGen.next
          intercept[RefundProcessingException] {
            action(source).await
          }
          ()
        }
      }
    }
  }

}
