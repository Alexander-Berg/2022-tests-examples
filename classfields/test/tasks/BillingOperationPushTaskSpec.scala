package ru.yandex.vertis.billing.tasks

import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import org.scalatest.Assertion
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.BilledEventDivisionDaoResolver
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.model_core.gens.{orderTransactionGen, OrderTransactionGenParams, Producer}
import ru.yandex.vertis.billing.model_core.{Epoch, OrderId, OrderTransaction, OrderTransactions}
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.service.async.{
  AsyncOrderService,
  AsyncTypedKeyValueService,
  AsyncTypedKeyValueServiceWrapper
}
import ru.yandex.vertis.billing.service.delivery.MessageDeliveryService
import ru.yandex.vertis.billing.util.{AutomatedContext, BilledEventPushTaskSpecBase}
import ru.yandex.vertis.billing.util.mock.{
  AsyncOrderServiceMockBuilder,
  MessageDeliveryServiceMockBuilder,
  TypedKeyValueServiceMockBuilder
}

import scala.annotation.nowarn

/**
  * @author tolmach
  */
@nowarn("msg=discarded non-Unit value")
class BillingOperationPushTaskSpec extends AsyncSpecBase with BilledEventPushTaskSpecBase with JdbcSpecTemplate {

  private val PayloadBatchesPerDivision = 10
  private val PayloadsPerDivision = BillingOperationPushTask.WithdrawBatchSize * PayloadBatchesPerDivision

  private def runTask(
      domain: String,
      orderService: AsyncOrderService,
      typedKeyValueService: AsyncTypedKeyValueService,
      messageDeliveryService: MessageDeliveryService,
      billedEventDaoResolver: BilledEventDivisionDaoResolver) = {
    val task = new BillingOperationPushTask(
      domain,
      orderService,
      typedKeyValueService,
      messageDeliveryService,
      billedEventDaoResolver
    )

    task.execute(ConfigFactory.empty())
  }

  private def checkTask(
      domain: String,
      orderService: AsyncOrderService,
      typedKeyValueService: AsyncTypedKeyValueService,
      messageDeliveryService: MessageDeliveryService,
      billedEventDaoResolver: BilledEventDivisionDaoResolver): Unit = {

    val result = runTask(
      domain,
      orderService,
      typedKeyValueService,
      messageDeliveryService,
      billedEventDaoResolver
    )

    result.futureValue
  }

  private def checkTaskFail(
      domain: String,
      orderService: AsyncOrderService,
      typedKeyValueService: AsyncTypedKeyValueService,
      messageDeliveryService: MessageDeliveryService,
      billedEventDaoResolver: BilledEventDivisionDaoResolver,
      expectedReason: Throwable): Assertion = {

    val result = runTask(
      domain,
      orderService,
      typedKeyValueService,
      messageDeliveryService,
      billedEventDaoResolver
    )

    val actualReason = result.failed.futureValue
    actualReason shouldBe expectedReason
  }

  private val NonWithdrawTransactionTypes =
    Set(OrderTransactions.Incoming, OrderTransactions.Rebate, OrderTransactions.Correction)

  private def nonWithdrawTransactionGen(orderIds: Seq[OrderId]): Gen[OrderTransaction] = {
    for {
      tpe <- Gen.oneOf(NonWithdrawTransactionTypes.toSeq)
      orderId <- Gen.oneOf(orderIds)
      params = OrderTransactionGenParams().withType(tpe).withOrderId(orderId)
      tr <- orderTransactionGen(params)
    } yield tr
  }

  "BillingOperationPushTaskSpec" should {
    "work correctly" when {
      "empty data for both types passed" in {
        runOnEmptyPreparedData { case PreparedData(domain, neededDivisionsWithInfoMap, billedEventDaoResolver, _, _) =>
          val nonWithdrawEpoch = 0

          val typedKeyValueServiceMockBuilder = prepare(
            TypedKeyValueServiceMockBuilder(),
            neededDivisionsWithInfoMap,
            BillingOperationPushTask.WithdrawBatchSize,
            BillingOperationPushTask.withdrawEpochMarker
          )
          val completedTypedKeyValueServiceMockBuilder = typedKeyValueServiceMockBuilder
            .withGetMock[Epoch](BillingOperationPushTask.NonWithdrawEpochMarker, nonWithdrawEpoch)
          val typedKeyValueService = completedTypedKeyValueServiceMockBuilder.build
          val asyncTypedKeyValueService = new AsyncTypedKeyValueServiceWrapper(typedKeyValueService)

          val messageDeliveryService = MessageDeliveryServiceMockBuilder().build

          val pushTaskRC = AutomatedContext("BillingOperationPushTask")
          val orderService = AsyncOrderServiceMockBuilder()
            .withGetModifiedSinceTransactions(nonWithdrawEpoch, NonWithdrawTransactionTypes, Seq.empty)(pushTaskRC)
            .build

          checkTask(
            domain,
            orderService,
            asyncTypedKeyValueService,
            messageDeliveryService,
            billedEventDaoResolver
          )
        }
      }
      "process empty withdraw but fail because of non withdraw process fail" in {
        runOnEmptyPreparedData { case PreparedData(domain, neededDivisionsWithInfoMap, billedEventDaoResolver, _, _) =>
          val nonWithdrawEpoch = 0

          val typedKeyValueServiceMockBuilder = prepare(
            TypedKeyValueServiceMockBuilder(),
            neededDivisionsWithInfoMap,
            BillingOperationPushTask.WithdrawBatchSize,
            BillingOperationPushTask.withdrawEpochMarker
          )
          val completedTypedKeyValueServiceMockBuilder = typedKeyValueServiceMockBuilder
            .withGetMock[Epoch](BillingOperationPushTask.NonWithdrawEpochMarker, nonWithdrawEpoch)
          val typedKeyValueService = completedTypedKeyValueServiceMockBuilder.build
          val asyncTypedKeyValueService = new AsyncTypedKeyValueServiceWrapper(typedKeyValueService)

          val messageDeliveryService = MessageDeliveryServiceMockBuilder().build

          val pushTaskRC = AutomatedContext("BillingOperationPushTask")
          val failure = new RuntimeException("artificial")
          val orderService = AsyncOrderServiceMockBuilder()
            .withGetModifiedSinceTransactions(nonWithdrawEpoch, NonWithdrawTransactionTypes, failure)(pushTaskRC)
            .build

          checkTaskFail(
            domain,
            orderService,
            asyncTypedKeyValueService,
            messageDeliveryService,
            billedEventDaoResolver,
            failure
          )
        }
      }
      "process empty non withdraw but fail because of withdraw process fail" in {
        runOnEmptyPreparedData { case PreparedData(domain, neededDivisionsWithInfoMap, billedEventDaoResolver, _, _) =>
          val nonWithdrawEpoch = 0

          val typedKeyValueServiceMockBuilder = prepare(
            TypedKeyValueServiceMockBuilder(),
            neededDivisionsWithInfoMap,
            BillingOperationPushTask.WithdrawBatchSize,
            BillingOperationPushTask.withdrawEpochMarker
          )
          val completedTypedKeyValueServiceMockBuilder = typedKeyValueServiceMockBuilder
            .withGetMock[Epoch](BillingOperationPushTask.NonWithdrawEpochMarker, nonWithdrawEpoch)
          val typedKeyValueService = completedTypedKeyValueServiceMockBuilder.build
          val asyncTypedKeyValueService = new AsyncTypedKeyValueServiceWrapper(typedKeyValueService)

          val messageDeliveryService = MessageDeliveryServiceMockBuilder().build

          val pushTaskRC = AutomatedContext("BillingOperationPushTask")
          val orderService = AsyncOrderServiceMockBuilder()
            .withGetModifiedSinceTransactions(nonWithdrawEpoch, NonWithdrawTransactionTypes, Seq.empty)(pushTaskRC)
            .build

          checkTask(
            domain,
            orderService,
            asyncTypedKeyValueService,
            messageDeliveryService,
            billedEventDaoResolver
          )
        }
      }
      "correct data for both types passed" in {
        runOnPreparedData(eventStorageDatabase, PayloadsPerDivision, BillingOperationPushTask.WithdrawBatchSize) {
          case PreparedData(
                domain,
                neededDivisionsWithInfoMap,
                billedEventDaoResolver,
                ordersMap,
                enrichedWrappedMap
              ) =>
            val orderIds = ordersMap.keys.toSeq
            val nonWithdrawTransactions = nonWithdrawTransactionGen(orderIds).next(1000)
            val nonWithdrawFirstEpoch = 0
            val nonWithdrawLastEpoch = nonWithdrawTransactions.flatMap(_.epoch).max
            val expectedNonWithdrawForSend = nonWithdrawTransactions.map { tr =>
              val order = ordersMap(tr.orderId)
              val protoDomain = BillingOperationPushTask.toProtoDomain(domain, order)
              Conversions.toBillingOperation(tr, order, protoDomain).get
            }

            val enrichedBatchesMap = enrichedWrappedMap.view.mapValues { enriched =>
              val grouped = enriched.grouped(1000)
              grouped.map(_.flatten)
            }.toMap
            val enrichedBatches = enrichedBatchesMap.values.flatten

            val expectedWithdrawForSendBatches = enrichedBatches.map { batch =>
              batch.map(Conversions.toBillingOperation)
            }

            val typedKeyValueServiceMockBuilder = prepare(
              TypedKeyValueServiceMockBuilder(),
              neededDivisionsWithInfoMap,
              BillingOperationPushTask.WithdrawBatchSize,
              BillingOperationPushTask.withdrawEpochMarker
            )

            val completedTypedKeyValueServiceMockBuilder = typedKeyValueServiceMockBuilder
              .withGetMock[Epoch](BillingOperationPushTask.NonWithdrawEpochMarker, nonWithdrawFirstEpoch)
              .withSetMock[Epoch](BillingOperationPushTask.NonWithdrawEpochMarker, nonWithdrawLastEpoch)
            val typedKeyValueService = completedTypedKeyValueServiceMockBuilder.build
            val asyncTypedKeyValueService = new AsyncTypedKeyValueServiceWrapper(typedKeyValueService)

            val messageDeliveryServiceBuilder = MessageDeliveryServiceMockBuilder()
            val messageDeliveryServiceBuilderWithNonWithdrawSendBatch =
              messageDeliveryServiceBuilder.withSendBatch(expectedNonWithdrawForSend.toSeq)
            val completedMessageDeliveryServiceBuilder =
              expectedWithdrawForSendBatches.foldLeft(messageDeliveryServiceBuilderWithNonWithdrawSendBatch) {
                case (builder, batch) =>
                  builder.withSendBatch(batch.toSeq)
              }
            val messageDeliveryService = completedMessageDeliveryServiceBuilder.build

            val processorRC = AutomatedContext("BilledEventProcessor")
            val pushTaskRC = AutomatedContext("BillingOperationPushTask")
            val orderService = AsyncOrderServiceMockBuilder()
              .withGetByOrderIds(ordersMap)(processorRC)
              .withGetByOrderIds(ordersMap)(pushTaskRC)
              .withGetModifiedSinceTransactions(
                nonWithdrawFirstEpoch,
                NonWithdrawTransactionTypes,
                nonWithdrawTransactions
              )(pushTaskRC)
              .build

            checkTask(
              domain,
              orderService,
              asyncTypedKeyValueService,
              messageDeliveryService,
              billedEventDaoResolver
            )
        }
      }
    }
  }

}
