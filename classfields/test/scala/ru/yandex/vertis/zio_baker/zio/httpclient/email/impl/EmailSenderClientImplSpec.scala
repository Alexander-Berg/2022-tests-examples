package ru.yandex.vertis.zio_baker.zio.httpclient.email.impl

import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{EmailSenderClientConfig, HttpClientConfig}
import ru.yandex.vertis.zio_baker.zio.httpclient.email.EmailSenderRequest.{Attachment, MimeType}
import ru.yandex.vertis.zio_baker.zio.httpclient.email._
import zio.test.Assertion.isUnit
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.ZLayer
import zio.blocking.Blocking

object EmailSenderClientImplSpec extends DefaultRunnableSpec {

  private val clientConfig: EmailSenderClientConfig = EmailSenderClientConfig(
    http = HttpClientConfig(
      url = "http://email-sender-proxy-api.vrts-slb.test.vertis.yandex.net"
    ),
    key = "[...place key here...]",
    service = "autoru",
    senderName = "shark"
  )

  private lazy val httpClientBackendLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(clientConfig.http) >>> HttpClient.blockingLayer

  private lazy val clientLayer =
    httpClientBackendLayer ++
      ZLayer.succeed(clientConfig) ++
      ZLayer.succeed(Environments.Testing) >>>
      EmailSenderClient.live

  private val templateName: String = "promocode_send_4_external_panorama"
  private val recipientEmail: EmailAddress = "slider5@yandex-team.ru"

  private val request: EmailSenderRequest = EmailSenderRequest(
    to = Seq(EmailSenderRequest.Recipient(recipientEmail)),
    args = Map(
      "argument1" -> "argumentValue1",
      "argument2" -> "argumentValue2"
    ),
    attachments = Seq(
      Attachment.fromString(
        data = "attachment file content...",
        mimeType = MimeType.TextPlain,
        filename = "attachment.txt"
      )
    ),
    headers = Map(
      "header1" -> "headerValue1",
      "header2" -> "headerValue2"
    ),
    async = false
  )

  def spec: ZSpec[TestEnvironment, Any] =
    suite("EmailSenderClient")(
      testM("sendEmail") {
        assertM(EmailSenderClient.sendEmail(templateName, request))(isUnit)
          .provideLayer(clientLayer)
      }
    ) @@ ignore
}
