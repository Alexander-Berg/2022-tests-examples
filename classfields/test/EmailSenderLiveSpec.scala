package common.clients.email.test

import common.clients.email.model.{EmailSenderConfig, EmailSenderTemplate}
import common.clients.email.{EmailSender, EmailSenderLive}
import common.zio.sttp.endpoint.Endpoint
import io.circe.{Json, JsonObject}
import common.zio.app.Environments
import common.zio.logging.Logging
import common.zio.sttp.Sttp
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.{Response, StringBody}
import sttp.model.MediaType
import zio._
import zio.test.Assertion._
import zio.test._

object EmailSenderLiveSpec extends DefaultRunnableSpec {

  private val NotFoundStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespondNotFound()

  private object emailReplacement {
    val someEmail = "some@ya.ru"
    val yateamEmail = "some@yandex-team.ru"
    val map = Map(someEmail -> yateamEmail)
  }

  private val testEmailSenderConfig =
    EmailSenderConfig("test-login", "test-service", "test-sender", Some(emailReplacement.map))
  private val testEmailSenderConfigLayer = ZLayer.succeed(testEmailSenderConfig)

  private val senderTemplate =
    EmailSenderTemplate("test-template", JsonObject("test-param" -> Json.fromString("test-json")))
  private val testEmail = "test@email.com"

  private def validStub(stubbedEmail: String) = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case r
        if r.uri.path.equals(
          List("api", "0", testEmailSenderConfig.service, "transactional", senderTemplate.name, "send")
        ) &&
          r.uri.params.toMap == Map("to_email" -> stubbedEmail) &&
          r.body == StringBody("""{"args":{"test-param":"test-json"}}""", "utf-8", MediaType.ApplicationJson) =>
      Response.ok(())
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("EmailSenderLive")(
      testM("die on error") {
        for {
          response <- EmailSender.sendEmail(senderTemplate, testEmail).run
        } yield assert(response)(
          fails(
            hasMessage(
              containsString(
                "email_sender.send_email request: 'http://host:2000/api/0/test-service/transactional/test-template/send?to_email=test@email.com' failed with code 404 and body [Not found]"
              )
            )
          )
        )
      }.provideCustomLayer(createEnvironment(NotFoundStub)),
      testM("not fail on valid response") {
        for {
          response <- EmailSender.sendEmail(senderTemplate, testEmail)
        } yield assert(response)(isUnit)
      }.provideCustomLayer(createEnvironment(validStub(testEmail))),
      testM("replace email if replacement is defined and env != prod") {
        EmailSender.sendEmail(senderTemplate, emailReplacement.someEmail).map(assert(_)(isUnit))
      }.provideCustomLayer(createEnvironment(validStub(emailReplacement.yateamEmail), env = Environments.Testing)),
      testM("do not replace email if replacement is defined but env == prod") {
        EmailSender.sendEmail(senderTemplate, emailReplacement.someEmail).map(assert(_)(isUnit))
      }.provideCustomLayer(createEnvironment(validStub(emailReplacement.someEmail), env = Environments.Stable))
    )
  }

  def createEnvironment(
      sttpBackendStub: Sttp.ZioSttpBackendStub,
      env: Environments.Value = Environments.Stable): ZLayer[Any, Nothing, Has[EmailSender.Service]] = {

    (Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpBackendStub) ++ testEmailSenderConfigLayer ++
      ZLayer.succeed(env) ++ Logging.live) >>>
      ZLayer.fromServices[
        Endpoint,
        Sttp.Service,
        EmailSenderConfig,
        Environments.Value,
        Logging.Service,
        EmailSender.Service
      ](
        new EmailSenderLive(_, _, _, _, _)
      )
  }
}
