package ru.yandex.vertis.billing.integration.test.mocks

import com.google.protobuf.Message
import ru.yandex.vertis.billing.service.delivery.MessageDeliveryService

import scala.concurrent.Future
import scala.reflect.ClassTag

class TestMessageDeliveryService extends MessageDeliveryService {

  type ClassName = String

  private var messages = Map[ClassName, List[Message]]()

  override def send[T <: Message: ClassTag](message: T): Future[Unit] = synchronized {
    val className = message.getClass.getName
    val newMessages = messages.getOrElse(className, Nil) :+ message
    messages = messages.updated(className, newMessages)
    Future.unit
  }

  override def sendBatch[T <: Message: ClassTag](batch: Seq[T]): Future[Unit] = synchronized {
    batch.toList match {
      case message :: _ =>
        val className = message.getClass.getName
        val newMessages = messages.getOrElse(className, Nil) ++ batch
        messages = messages.updated(className, newMessages)
        Future.unit
      case Nil =>
        Future.unit
    }
  }

  def getMessages[M <: Message: ClassTag]: Seq[M] = synchronized {
    val className = implicitly[ClassTag[M]].runtimeClass.getName
    messages.getOrElse(className, Nil).map(_.asInstanceOf[M])
  }

  def clear(): Unit = synchronized {
    messages = Map.empty
  }

}
