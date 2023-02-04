package ru.auto.salesman.test.proto.user

import com.google.protobuf.{Timestamp, Value}
import ru.auto.salesman.model.user.PaymentRequestRaw
import ru.yandex.vertis.banker.model.EventsModel
import ru.yandex.vertis.banker.model.EventsModel.PaymentNotification
import ru.yandex.vertis.protobuf.ProtoWriter

object PaymentRequestWriter
    extends ProtoWriter[PaymentRequestRaw, EventsModel.PaymentNotification] {

  def write(obj: PaymentRequestRaw): EventsModel.PaymentNotification = {
    import scala.collection.JavaConverters._
    val fields = Map(
      "transaction" -> obj.payload.transactionId,
      "domain" -> obj.payload.domain
    ).map { case (k, v) =>
      k -> Value.newBuilder().setStringValue(v).build()
    }.asJava

    val timestamp = Timestamp
      .newBuilder()
      .setSeconds(obj.time.toInstant.getMillis / 1000)
      .setNanos(obj.time.getMillisOfSecond * 1e6.toInt)
    val mappedAction = PaymentNotification.Action.values
      .find(_.name().equalsIgnoreCase(obj.action.toString))
      .get
    val b = PaymentNotification.newBuilder()
    b.setId(obj.bankerTransactionId)
      .setTimestamp(timestamp)
      .setAction(mappedAction)
      .getPayloadBuilder
      .getStructBuilder
      .putAllFields(fields)
    b.build()
  }
}
