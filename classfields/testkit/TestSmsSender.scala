package common.clients.sms.testkit

import java.util.UUID

import common.clients.sms.SmsSender
import common.clients.sms.SmsSender.SmsSender
import common.clients.sms.model.{SmsMessage, SmsParams}
import zio.macros.accessible
import zio.{Has, Ref, Task, UIO, ULayer}

import scala.collection.immutable.ListMap

@accessible
object TestSmsSender {

  type SmsSenderMock = Has[Service]

  case class Sent(message: SmsMessage, params: SmsParams)

  trait Service {
    def lastSent: UIO[Option[(String, Sent)]]

    def allSent: UIO[ListMap[String, Sent]]

    def reset: UIO[Unit]
  }

  val layer: ULayer[SmsSenderMock with SmsSender] = {
    Ref
      .make(ListMap.empty[String, Sent])
      .map { sent =>
        val test = new Test(sent)
        Has.allOf[Service, SmsSender.Service](test, test)
      }
      .toLayerMany

  }

  private class Test(sent: Ref[ListMap[String, Sent]]) extends Service with SmsSender.Service {
    override def lastSent: UIO[Option[(String, Sent)]] = sent.get.map(_.headOption)

    override def allSent: UIO[ListMap[String, Sent]] = sent.get

    override def reset: UIO[Unit] = sent.set(ListMap.empty)

    override def send(message: SmsMessage, params: SmsParams): Task[String] = {
      val id = UUID.randomUUID().toString
      sent.update(_ + (id -> Sent(message, params))).as(id)
    }
  }
}
