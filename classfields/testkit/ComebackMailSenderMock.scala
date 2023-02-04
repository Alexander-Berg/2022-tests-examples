package ru.auto.comeback.consumer.testkit

import ru.auto.comeback.sender.ComebackMailSender
import ru.auto.comeback.sender.ComebackMailSender.ComebackMailSender
import zio.test.mock
import zio.{Has, Task, URLayer, ZLayer}
import zio.test.mock.Mock

object ComebackMailSenderMock extends Mock[ComebackMailSender] {

  object SendEmails extends Effect[ComebackMailSender.NotificationData, Throwable, Unit]

  override val compose: URLayer[Has[mock.Proxy], ComebackMailSender] = ZLayer.fromService { proxy =>
    new ComebackMailSender.Service {
      override def sendEmails(notification: ComebackMailSender.NotificationData): Task[Unit] =
        proxy(SendEmails, notification)
    }
  }
}
