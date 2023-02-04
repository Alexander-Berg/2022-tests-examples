package common.clients.email.testkit

import common.clients.email.EmailSender.EmailSender
import common.clients.email.model.EmailSenderTemplate
import zio.test.mock
import zio.test.mock.Mock
import zio.{Has, URLayer, ZLayer}

object EmailSenderTest extends Mock[EmailSender] {

  object SendEmail extends Effect[(EmailSenderTemplate, String), Throwable, Unit]

  override val compose: URLayer[Has[mock.Proxy], EmailSender] =
    ZLayer.fromService { proxy => (template: EmailSenderTemplate, toEmail: String) =>
      proxy(SendEmail, template, toEmail)
    }
}
