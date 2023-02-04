package ru.auto.cabinet.service.passport

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import ru.auto.cabinet.remote.TvmServiceClient
import ru.auto.cabinet.trace.Context
import ru.auto.cabinet.util.TestServer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class HttpPassportClientSpec
    extends FlatSpec
    with Matchers
    with ScalaFutures
    with TestServer {
  implicit private val rc = Context.unknown

  implicit private val system: ActorSystem =
    ActorSystem("test-system", ConfigFactory.empty())

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    Span(10, Seconds)
  )

  private val tvmServiceClient = mock[TvmServiceClient]

  val userId = 1
  val clientId = 20101
  val sessionId = "123"
  val identity = Right(Phone("456"))
  val alias = "alias"

  "getUserFullName" should "return alias" in {

    val json = Source
      .fromInputStream(
        getClass.getResourceAsStream("/passport/getUserFullName.json")
      )
      .mkString

    when(tvmServiceClient.getServiceTicket())
      .thenReturn("tvm-ticket")

    withServer {
      (get & pathPrefix(
        "api" / "2.x" / "auto" / "users" / userId.toString
      )) {
        complete(
          HttpEntity(ContentTypes.`application/json`, json)
        )
      }
    } { address =>
      new HttpPassportClient(
        PassportClientSettings(address.map(_.toString).futureValue),
        tvmServiceClient
      ).getUserFullName(userId).futureValue shouldBe Some(alias)
    }
  }

  "getClientUsers" should "return users" in {

    val json = Source
      .fromInputStream(
        getClass.getResourceAsStream("/passport/getClientUsers.json")
      )
      .mkString

    when(tvmServiceClient.getServiceTicket())
      .thenReturn("tvm-ticket")

    withServer {
      (get & pathPrefix(
        "api" / "2.x" / "auto" / "clients" / "id" / clientId.toString / "users"
      )) {
        complete(
          HttpEntity(ContentTypes.`application/json`, json)
        )
      }
    } { address =>
      val client = new HttpPassportClient(
        PassportClientSettings(address.map(_.toString).futureValue),
        tvmServiceClient
      )

      client.getClientUsers(clientId, withProfile = true).futureValue shouldBe {
        ClientUsersResponse(
          userIds = Some(List("11296277", "69130186")),
          users = Some(
            List(
              User(
                id = "11296277",
                profile = Profile(AutoruProfile(alias = None)),
                emails = Some(List(Email("11296277@auto.ru")))
              ),
              User(
                id = "69130186",
                profile = Profile(AutoruProfile(alias = None)),
                emails = Some(List(Email("69130186@auto.ru")))
              )
            ))
        )
      }
    }
  }

  "getSession" should "return session.id" in {
    val json = Source
      .fromInputStream(
        getClass.getResourceAsStream("/passport/getSession.json")
      )
      .mkString

    when(tvmServiceClient.getServiceTicket())
      .thenReturn("tvm-ticket")

    withServer {
      (post & pathPrefix(
        "api" / "2.x" / "auto" / "auth" / "login-or-register-int"
      )) {
        complete(
          HttpEntity(ContentTypes.`application/json`, json)
        )
      }
    } { address =>
      new HttpPassportClient(
        PassportClientSettings(address.map(_.toString).futureValue),
        tvmServiceClient
      ).getSession(identity)
        .map(_.session.id)
        .futureValue shouldBe sessionId
    }
  }

  "findUsers" should "return users" in {
    val json = Source
      .fromInputStream(
        getClass.getResourceAsStream("/passport/findUsers.json")
      )
      .mkString

    when(tvmServiceClient.getServiceTicket())
      .thenReturn("tvm-ticket")

    withServer {
      (get & pathPrefix(
        "api" / "2.x" / "auto" / "users" / "search"
      )) {
        complete(
          HttpEntity(ContentTypes.`application/json`, json)
        )
      }
    } { address =>
      new HttpPassportClient(
        PassportClientSettings(address.map(_.toString).futureValue),
        tvmServiceClient
      ).findUsers(identity)
        .map(_.headOption.flatMap(_.profile.autoru.alias))
        .futureValue shouldBe Some(alias)
    }
  }
}
