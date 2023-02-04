package auto.dealers.multiposting.storage.testkit.gen

import auto.dealers.multiposting.model.{AvitoWalletOperation, AvitoWalletOperationServiceType, AvitoWalletOperationType}
import zio.test.Gen

import java.time.OffsetDateTime

object GenAvitoWalletOperation {

  import AvitoWalletOperationServiceType._

  def genOperationName(operationType: AvitoWalletOperationType) = operationType match {
    case AvitoWalletOperationType.Reservation =>
      Gen.elements("Покупка", "Изменение").flatMap(prefix => Gen.anyString.map(str => prefix + s" $str"))
    case AvitoWalletOperationType.ReservationReturn =>
      Gen.elements("Возврат").flatMap(prefix => Gen.anyString.map(str => prefix + s" $str"))
    case _ =>
      Gen.anyString
  }

  val genAvitoWalletOperation =
    for {
      operationId <- Gen.anyUUID
      clientId <- Gen.anyLong
      avitoOfferId <- Gen.anyLong
      days <- Gen.anyShort
      operationType <- Gen.elements(AvitoWalletOperationType.values: _*)
      operationName <- genOperationName(operationType)
      serviceIdOpt <- Gen.option(Gen.anyLong)
      serviceNameOpt <- Gen.anyString.map(str => serviceIdOpt.map(_ => str))
      serviceTypeOpt <- Gen
        .elements(AvitoWalletOperationServiceType.values: _*)
        .map(serviceType => serviceIdOpt.map(_ => serviceType))
      amountRub <- Gen.anyLong
      amountBonus <- if (amountRub != 0) Gen.anyLong else Gen.const(0L)
    } yield AvitoWalletOperation(
      operationId = operationId.toString,
      clientId = clientId,
      createdAt = OffsetDateTime.now().plusDays(days),
      operationType = operationType.value,
      operationName = operationName,
      serviceId = serviceIdOpt,
      serviceName = serviceNameOpt,
      serviceType = serviceTypeOpt.map(_.value),
      amountTotal = amountBonus + amountRub,
      amountRub = amountBonus,
      amountBonus = amountRub,
      avitoOfferId = Some(avitoOfferId),
      isPlacement = serviceTypeOpt.getFlags.isPlacement,
      isVas = serviceTypeOpt.getFlags.isVas,
      isOther = serviceTypeOpt.getFlags.isVas
    )

  val genAvitoWalletOperationPlacementTariff = {
    import AvitoWalletOperationType._
    for {
      operation <- genAvitoWalletOperation
      operationType <- Gen.elements(Reservation, ReservationReturn)
      operationName <- genOperationName(operationType)
      serviceId <- Gen.anyLong
      serviceName <- Gen.anyString
      serviceType <- Gen.const(AvitoWalletOperationServiceType.Tariff)
    } yield operation
      .copy(
        operationType = operationType.value,
        operationName = operationName,
        serviceId = Some(serviceId),
        serviceName = Some(serviceName),
        serviceType = Some(serviceType.value),
        isOther = false,
        isPlacement = true,
        isVas = false
      )
  }

  val genAvitoWalletNotPlacementOperation = {
    for {
      operation <- genAvitoWalletOperation
      serviceType <- Gen
        .elements(
          AvitoWalletOperationServiceType.values.filter(_ != AvitoWalletOperationServiceType.Tariff): _*
        )
        .map(t => operation.serviceId.map(_ => t))
    } yield operation
      .copy(
        serviceType = serviceType.map(_.value)
      )
  }
}
