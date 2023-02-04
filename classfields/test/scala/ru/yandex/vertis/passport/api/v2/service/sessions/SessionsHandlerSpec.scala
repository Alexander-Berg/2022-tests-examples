package ru.yandex.vertis.passport.api.v2.service.sessions

import akka.http.scaladsl.model.StatusCodes._
import org.scalatest.WordSpec
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel.SimpleErrorResponse
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{DomainDirectives, MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  * @author zvez
  */
class SessionsHandlerSpec extends WordSpec with RootedSpecBase with MockedBackend with V2Spec with NoTvmAuthorization {

  val base = "/api/2.x/auto/sessions"

  "get session by id" should {

    "response NotFound on wrong sid" in {
      Get(s"$base/wrong") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe NotFound
        }
    }

    "response NotFound for non-existent session" in {
      val sid = ModelGenerators.fakeSessionId.next
      when(sessionFacade.get(eq(Some(sid)), ?)(?))
        .thenReturn(Future.failed(new NoSuchElementException("Session was not found")))
      Get(s"$base/${sid.asString}") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe NotFound
          contentType shouldBe expectedContentType
          responseAs[SimpleErrorResponse].getError.getMessage should not be empty
        }
    }

    "response with session result" in {
      val sid = ModelGenerators.fakeSessionId.next
      val sessionResult = ModelGenerators.sessionResult.next
      when(sessionFacade.get(eq(Some(sid)), ?)(?)).thenReturn(Future.successful(sessionResult))

      Get(s"$base/${sid.asString}") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.SessionResult]
          checkSessionResult(sessionResult, response)
        }
    }

    "return parent session" in {
      val sessionResult =
        ModelGenerators.sessionResult.filter(_.session.parentSession.isDefined).next
      val sid = sessionResult.session.id
      when(sessionFacade.get(eq(Some(sid)), ?)(?)).thenReturn(Future.successful(sessionResult))

      Get(s"$base/${sid.asString}") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.SessionResult]
          checkSessionResult(sessionResult, response)
          response.getSession.hasParentSession shouldBe true
        }

    }
  }

  "create anonymous session" should {
    "response with session result (no deviceUid)" in {
      val source =
        ModelGenerators.anonymousSessionSource
          .filter(v => v.deviceUid.isEmpty && v.ttl.isDefined)
          .next
      val sessionResult = ModelGenerators.sessionResult.next
      when(sessionFacade.createAnonymous(eq(source))(?))
        .thenReturn(Future.successful(sessionResult))

      Post(s"$base?ttl=${source.ttl.get.getStandardSeconds}") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.SessionResult]
          checkSessionResult(sessionResult, response)
        }
    }

    "response with session result (deviceUid in header)" in {
      val source =
        ModelGenerators.anonymousSessionSource
          .filter(v => v.deviceUid.isDefined && v.ttl.isDefined)
          .next
      val sessionResult = ModelGenerators.sessionResult.next
      when(sessionFacade.createAnonymous(eq(source))(?))
        .thenReturn(Future.successful(sessionResult))

      Post(s"$base/?ttl=${source.ttl.get.getStandardSeconds}") ~>
        commonHeaders ~>
        addHeader(DomainDirectives.DeviceUidHeader, source.deviceUid.get) ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.SessionResult]
          checkSessionResult(sessionResult, response)
        }
    }
  }

  "delete session by id" should {
    "response OK" in {
      val sid = ModelGenerators.fakeSessionId.next
      when(sessionFacade.delete(eq(Some(sid)))(?)).thenReturn(Future.unit)

      Delete(s"$base/${sid.asString}") ~>
        route ~>
        check {
          status shouldBe OK
        }
    }
  }

}
