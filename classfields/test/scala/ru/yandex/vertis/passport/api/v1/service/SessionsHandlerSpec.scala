package ru.yandex.vertis.passport.api.v1.service

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes}
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.api.v1.SessionTestUtils
import ru.yandex.vertis.passport.api.{DomainDirectives, MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.view.SessionResultView

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  * @author zvez
  */
class SessionsHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with SessionTestUtils
  with NoTvmAuthorization {

  val commonHeaders = addHeader(Accept(MediaTypes.`application/json`))
  val base = "/api/1.x/service/auto/sessions"

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
      when(sessionFacade.get(eq(Some(sid)), ?)(?)).thenReturn(Future.failed(new NoSuchElementException))

      Get(s"$base/${sid.asString}") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe NotFound
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
          contentType shouldBe ContentTypes.`application/json`
          val response = responseAs[SessionResultView]
          checkSessionResult(sessionResult, response)
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

      Post(s"$base/?ttl=${source.ttl.get.getStandardSeconds}") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          val response = responseAs[SessionResultView]
          checkSessionResult(sessionResult, response)
        }
    }

    "response with session result (deviceUid in query)" in {
      val source =
        ModelGenerators.anonymousSessionSource
          .filter(v => v.deviceUid.isDefined && v.ttl.isDefined)
          .next
      val sessionResult = ModelGenerators.sessionResult.next
      when(sessionFacade.createAnonymous(eq(source))(?))
        .thenReturn(Future.successful(sessionResult))

      Post(s"$base/?ttl=${source.ttl.get.getStandardSeconds}&deviceUid=${source.deviceUid.get}") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          val response = responseAs[SessionResultView]
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
          val response = responseAs[SessionResultView]
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
