package auto.dealers.booking.testkit

import java.time.Instant

import com.google.protobuf.struct.Struct
import common.scalapb.ScalaProtobuf
import zio.test.Gen
import auto.dealers.booking.testkit.gen._

object BankingModelGens {

  import ru.yandex.vertis.banker.model.common_model.OpaquePayload
  import ru.yandex.vertis.banker.model.common_model.OpaquePayload.Impl
  import ru.yandex.vertis.banker.model.events_model.PaymentNotification.Action
  import ru.yandex.vertis.banker.model.events_model.PaymentNotification

  val defaultFields = Map("domain" -> "booking", "transaction" -> "test_transaction_id")

  def structFieldsGen(fieldsGen: RGen[Map[String, String]] = defaultFields): RGen[Struct] = {
    fieldsGen.map { fields =>
      Struct.of(fields.map { case (k, v) => k -> string2Value(v) })
    }
  }

  def structPayloadGen(fieldsGen: Map[String, RGen[String]]): RGen[Impl.Struct] = {
    val mapGen = fieldsGen.foldLeft[RGen[Map[String, String]]](Gen.const(Map.empty[String, String])) {
      case (agg, (k, vGen)) =>
        for {
          v <- vGen
          res <- agg
        } yield res + (k -> v)
    }
    structFieldsGen(mapGen).map(Impl.Struct.apply)
  }

  val stringPayloadGen: RGen[Impl] = Gen.anyString.map(Impl.Plain)
  val emptyPayloadGen: RGen[Impl] = Gen.const(Impl.Empty)

  val refundPayloadGen: RGen[Impl] = for {
    user <- Gen.anyString
    comment <- Gen.anyString
    struct <- structFieldsGen()
  } yield Impl.RefundPayload(OpaquePayload.RefundPayload(user, comment, Some(struct)))

  def paymentNotificationGen(
      paidAtGen: RGen[String] = Instant.now().toString,
      domainGen: RGen[String] = "booking",
      actionGen: RGen[Action] = Action.ACTIVATE,
      bookingTransactionId: RGen[String] = "test_transaction_id",
      bankerTransactionId: RGen[String] = Gen.anyUUID.map(_.toString)
    )(payloadImplGen: RGen[Impl] = {
        structPayloadGen(Map("domain" -> domainGen, "transaction" -> bookingTransactionId))
      }): RGen[PaymentNotification] = {
    for {
      id <- bankerTransactionId
      paidAt <- paidAtGen.map(Instant.parse)

      payloadImpl <- payloadImplGen
      action <- actionGen
    } yield {
      PaymentNotification(
        id = id,
        timestamp = Some(ScalaProtobuf.instantToTimestamp(paidAt)),
        action = action,
        payload = Some(OpaquePayload(payloadImpl))
      )
    }
  }
}
