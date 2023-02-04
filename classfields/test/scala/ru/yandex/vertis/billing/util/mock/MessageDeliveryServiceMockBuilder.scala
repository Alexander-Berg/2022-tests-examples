package ru.yandex.vertis.billing.util.mock

import com.google.protobuf.Message
import ru.yandex.vertis.billing.service.delivery.MessageDeliveryService

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * @author tolmach
  */
case class MessageDeliveryServiceMockBuilder(
    sendStubs: Seq[PartialFunction[(Message, ClassTag[Message]), Future[Unit]]] = Seq.empty,
    sendBatchStubs: Seq[PartialFunction[(Seq[Message], ClassTag[Message]), Future[Unit]]] = Seq.empty)
  extends MockBuilder[MessageDeliveryService] {

  def withSendMock[T <: Message: ClassTag](message: T): MessageDeliveryServiceMockBuilder = {
    val stub: PartialFunction[(Message, ClassTag[Message]), Future[Unit]] = { case (`message`, _) =>
      Future.unit
    }
    this.copy(sendStubs = this.sendStubs :+ stub)
  }

  def withSendBatch[T <: Message: ClassTag](batch: Seq[T]): MessageDeliveryServiceMockBuilder = {
    val stub: PartialFunction[(Seq[Message], ClassTag[Message]), Future[Unit]] = {
      case (actualBatch, _) if actualBatch.size == batch.size && actualBatch.forall(batch.contains) => Future.unit
    }
    this.copy(sendBatchStubs = this.sendBatchStubs :+ stub)
  }

  def build: MessageDeliveryService = {
    val m: MessageDeliveryService = mock[MessageDeliveryService]

    val sendStub = sendStubs.reduceOption(_.orElse(_))

    sendStub.foreach { sendStub =>
      stub(m.send(_: Message)(_: ClassTag[Message]))(sendStub)
    }

    val sendBatchStub = sendBatchStubs.reduceOption(_.orElse(_))

    sendBatchStub.foreach { sendBatchStub =>
      stub(m.sendBatch(_: Seq[Message])(_: ClassTag[Message]))(sendBatchStub)
    }

    m
  }

}
