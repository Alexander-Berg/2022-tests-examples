package ru.yandex.vertis.billing.integration.test.mocks

import akka.Done
import akka.stream.scaladsl.Sink
import com.google.protobuf.Message
import ru.yandex.vertis.billing.kafka.KafkaProtoProducer

import scala.concurrent.Future

class TestKafkaProtoProducer[M <: Message] extends KafkaProtoProducer[M] {

  private var messages: List[M] = List.empty

  override val sink: Sink[M, Future[Done]] = Sink.foreach { message =>
    synchronized {
      messages = messages :+ message
    }
  }

  def getMessages = synchronized {
    messages
  }

  def clear() = synchronized {
    messages = List.empty[M]
  }
}
