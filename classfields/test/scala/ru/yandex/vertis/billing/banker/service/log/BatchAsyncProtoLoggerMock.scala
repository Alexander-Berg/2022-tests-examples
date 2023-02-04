package ru.yandex.vertis.billing.banker.service.log

import com.google.protobuf.Message

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * @author tolmach
  */
case class BatchAsyncProtoLoggerMock[M <: Message]() extends BatchAsyncProtoLogger[M] {

  private val history = ArrayBuffer.empty[M]

  override def logBatch(messages: Seq[M]): Future[Unit] = synchronized {
    history ++= messages
    Future.unit
  }

  def sentMessages: Seq[M] = synchronized {
    history.toList
  }

}
