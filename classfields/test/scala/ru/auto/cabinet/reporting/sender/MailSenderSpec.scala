package ru.auto.cabinet.reporting.sender

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.{AnyFlatSpecLike => FlatSpecLike}
import ru.auto.cabinet.environment.EnvironmentType
import ru.auto.cabinet.reporting.PublishingException

import scala.concurrent.duration._

/** Testing of [[MailSender]]
  */
class MailSenderSpec
    extends FlatSpecLike
    with Matchers
    with MockFactory
    with HttpEmulationHarness
    with IntegrationPatience {

  behavior.of("MailSender")

  it should "resend on 500 code" in {
    val statusCodeToReturn = stubFunction[Unit, StatusCode]

    // setting up mocks
    statusCodeToReturn
      .when(())
      .returning(StatusCodes.InternalServerError)
      .twice()
    statusCodeToReturn.when(()).returning(StatusCodes.OK).once()

    withHttpEmulation(
      post {
        complete(statusCodeToReturn(()) -> "Hello world!")
      }
    ) { (host, as, am) =>
      val mailSender = new MailSender(
        SenderSettings(
          host,
          "hello",
          "hello",
          20.seconds,
          securedConnection = false),
        EnvironmentType.Testing
      )(as, am)
      val res = mailSender
        .send(SendMail("hello@yandex-team.ru", "hello", "hello"))
        .futureValue

      res shouldBe SendResults.Ok
    }
  }

  it should "not resend on 200 code" in {
    val reqCounter = mockFunction[Unit, Unit]

    reqCounter.expects(()).once()

    withHttpEmulation(
      post {
        reqCounter(())
        complete(StatusCodes.OK -> "Hello world!")
      }
    ) { (host, as, am) =>
      val mailSender = new MailSender(
        SenderSettings(
          host,
          "hello",
          "hello",
          20.seconds,
          securedConnection = false),
        EnvironmentType.Testing
      )(as, am)
      val res = mailSender
        .send(SendMail("hello@yandex-team.ru", "hello", "hello"))
        .futureValue

      res shouldBe SendResults.Ok
    }
  }

  it should "not fail on invalid email" in {
    withHttpEmulation {
      post {
        complete {
          HttpResponse(
            StatusCodes.BadRequest,
            entity = HttpEntity(
              ContentType(`application/json`),
              """{"result":{"status":"ERROR","error":{"to_email":["Invalid value"]}}}"""))
        }
      }
    } { (host, as, am) =>
      val mailSender = new MailSender(
        SenderSettings(
          host,
          "hello",
          "hello",
          20.seconds,
          securedConnection = false),
        EnvironmentType.Testing
      )(as, am)
      val res = mailSender
        .send(SendMail("hello@yandex-team.ru", "hello", "hello"))
        .failed
        .futureValue
      res shouldBe a[InvalidEmailException]
    }
  }

  it should "fail on bad request, which isn't invalid email" in {
    withHttpEmulation {
      post {
        complete {
          HttpResponse(
            StatusCodes.BadRequest,
            entity = HttpEntity(
              ContentType(`application/json`),
              """{"result":{"status":"ERROR","error":{""" +
                // build large entity to force IllegalStateException
                // see https://github.com/akka/akka-http/issues/745#issuecomment-271571342 for details
                (1 to 10000)
                  .map { i =>
                    s""""other-$i": [""]"""
                  }
                  .mkString(",") +
                "}}}"
            )
          )
        }
      }
    } { (host, as, am) =>
      val mailSender = new MailSender(
        SenderSettings(
          host,
          "hello",
          "hello",
          20.seconds,
          securedConnection = false),
        EnvironmentType.Testing
      )(as, am)
      val res = mailSender
        .send(SendMail("hello@yandex-team.ru", "hello", "hello"))
        .failed
        .futureValue
      res shouldBe a[PublishingException]
    }
  }
}
