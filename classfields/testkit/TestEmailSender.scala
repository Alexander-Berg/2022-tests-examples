package common.clients.email.testkit

import common.clients.email.EmailSender
import common.clients.email.EmailSender.EmailSender
import common.clients.email.model.EmailSenderTemplate
import zio.macros.accessible
import zio.{Has, Ref, Task, UIO, ULayer}

@accessible
object TestEmailSender {
  type EmailSenderMock = Has[Service]

  case class Sent(template: EmailSenderTemplate, toEmail: String)

  trait Service {
    def lastSent: UIO[Option[Sent]]

    def allSent: UIO[Seq[Sent]]

    def reset: UIO[Unit]
  }

  val layer: ULayer[EmailSenderMock with EmailSender] = {
    Ref
      .make(List.empty[Sent])
      .map { sent =>
        val test = new Test(sent)
        Has.allOf[Service, EmailSender.Service](test, test)
      }
      .toLayerMany

  }

  private class Test(sent: Ref[List[Sent]]) extends Service with EmailSender.Service {
    override def lastSent: UIO[Option[Sent]] = sent.get.map(_.headOption)

    override def allSent: UIO[Seq[Sent]] = sent.get

    override def reset: UIO[Unit] = sent.set(Nil)

    override def sendEmail(template: EmailSenderTemplate, toEmail: String): Task[Unit] =
      sent.update(Sent(template, toEmail) :: _)
  }
}
