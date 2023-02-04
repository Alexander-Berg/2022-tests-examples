package auto.dealers.multiposting.storage.testkit

import auto.dealers.multiposting.model.{
  AvitoWalletOperation,
  AvitoWalletOperationMeta,
  ClientId,
  OperationId,
  PaymentType
}
import auto.dealers.multiposting.storage.AvitoWalletOperationMetaDao

import java.time.{OffsetDateTime, ZoneId}

object AvitoWalletOperationMetaDaoHelper {
  lazy val Now = OffsetDateTime.now().atZoneSimilarLocal(ZoneId.of("Europe/Moscow")).toOffsetDateTime
  lazy val CreateAtTwentyDaysAgo = Now.minusDays(20)
  lazy val CreateAtFifteenDaysAgo = Now.minusDays(15)
  lazy val CreateAtTenDaysAgo = Now.minusDays(10)
  lazy val CreateAtFiveDaysAgo = Now.minusDays(5)

  lazy val OperationIdToCreatedAtToPayedSumList: List[(OperationId, OffsetDateTime, Long)] = List(
    (OperationId("1"), CreateAtTwentyDaysAgo, 30000L),
    (OperationId("2"), CreateAtFifteenDaysAgo, 25000L),
    (OperationId("3"), CreateAtTenDaysAgo, 20000L),
    (OperationId("4"), CreateAtFiveDaysAgo, 15000L),
    (OperationId("5"), Now, 10000L)
  )

  def getOperationsWithMetas(
      operationIdToCreatedAtList: List[(OperationId, OffsetDateTime, Long)],
      avitoWalletOperations: List[AvitoWalletOperation],
      testClientId: ClientId,
      activeDays: Int,
      paymentTypeOpt: Option[PaymentType] = None,
      closeDate: Option[OffsetDateTime] = None): (List[AvitoWalletOperation], List[AvitoWalletOperationMeta]) = {
    operationIdToCreatedAtList
      .zip(avitoWalletOperations)
      .foldLeft((List.empty[AvitoWalletOperation], List.empty[AvitoWalletOperationMeta])) {
        case ((operationsAcc, operationsMetaAcc), ((id, createdAt, sum), operation)) =>
          val updatedOperation =
            operation.copy(
              operationId = id.value,
              amountRub = sum,
              createdAt = createdAt,
              clientId = testClientId.value
            )
          val paymentTypeIn = AvitoWalletOperationMetaDao.getOperationPaymentType(updatedOperation)

          val operationMeta =
            AvitoWalletOperationMeta(
              avitoWalletOperationId = OperationId(updatedOperation.operationId),
              activeFrom = updatedOperation.createdAt,
              activeTo = closeDate.getOrElse(updatedOperation.createdAt.plusDays(activeDays)),
              clientId = ClientId(updatedOperation.clientId),
              factAmountSumKopecks = updatedOperation.amountRub.toLong,
              paymentType = paymentTypeOpt.getOrElse(paymentTypeIn),
              refundAvitoWalletOperationId = None
            )

          (updatedOperation :: operationsAcc, operationMeta :: operationsMetaAcc)
      }
  }
}
