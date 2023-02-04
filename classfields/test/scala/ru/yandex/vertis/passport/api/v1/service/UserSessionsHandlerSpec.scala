package ru.yandex.vertis.passport.api.v1.service

import akka.http.scaladsl.model.MediaTypes
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import ru.yandex.vertis.passport.api.v1.SessionTestUtils
import ru.yandex.vertis.passport.model.UserSessionSource
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.view.SessionResultView

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class UserSessionsHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with SessionTestUtils
  with NoTvmAuthorization {

  val commonHeaders = addHeader(Accept(MediaTypes.`application/json`))
  val base = "/api/1.x/service/auto/users"

  private def correctSource(src: UserSessionSource) =
    src.copy(ttl = src.ttl.map(UserSessionsHandler.correctTtl))

  "create user's session" should {
    "response NotFound if no such user" in {
      val source = ModelGenerators.userSessionSource.filter(_.ttl.isDefined).next
      val expectedSource = correctSource(source)
      when(sessionFacade.createForUser(eq(expectedSource))(?))
        .thenReturn(Future.failed(new NoSuchElementException))

      Post(s"$base/${source.userId}/sessions?ttl=${source.ttl.get.getStandardSeconds}") ~>
        commonHeaders ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe NotFound
          }
        }
    }

    "response with session result" in {
      val source = ModelGenerators.userSessionSource.filter(_.ttl.isDefined).next
      val expectedSource = correctSource(source)
      val sessionResult = ModelGenerators.sessionResult.next

      when(sessionFacade.createForUser(eq(expectedSource))(?))
        .thenReturn(Future.successful(sessionResult))

      Post(s"$base/${source.userId}/sessions?ttl=${source.ttl.get.getStandardSeconds}") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          val response = responseAs[SessionResultView]
          checkSessionResult(sessionResult, response)
        }
    }

    "allow skip ttl" in {
      val source = ModelGenerators.userSessionSource.filter(_.ttl.isEmpty).next
      val expectedSource = correctSource(source)
      val sessionResult = ModelGenerators.sessionResult.next

      when(sessionFacade.createForUser(eq(expectedSource))(?))
        .thenReturn(Future.successful(sessionResult))

      Post(s"$base/${source.userId}/sessions") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          val response = responseAs[SessionResultView]
          checkSessionResult(sessionResult, response)
        }
    }
  }
}
