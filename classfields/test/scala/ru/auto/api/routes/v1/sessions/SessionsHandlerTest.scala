package ru.auto.api.routes.v1.sessions

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import org.mockito.Mockito.verify
import ru.auto.api.ApiSuite
import ru.auto.api.exceptions.SessionNotFoundException
import ru.auto.api.model.ModelGenerators
import ru.auto.api.services.MockedClients
import ru.auto.api.services.keys.TokenServiceImpl
import ru.yandex.passport.model.api.ApiModel.SessionResult

import scala.concurrent.Future

/**
  * @author zvez
  */
class SessionsHandlerTest extends ApiSuite with MockedClients {

  private val authHeader = addHeader("x-authorization", "Vertis " + TokenServiceImpl.swagger.value)
  private val commonHeaders = authHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  test("get current session: not found") {
    val sid = ModelGenerators.SessionIdGen.next

    when(passportClient.getSession(?)(?)).thenReturn(Future.failed(new SessionNotFoundException))
    when(passportClient.createAnonymousSession()(?))
      .thenReturn(Future.successful(ModelGenerators.SessionResultGen.next))
    Get(s"/1.0/session") ~>
      commonHeaders ~>
      addHeader("X-Session-Id", sid.value) ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        verify(passportClient).getSession(eq(sid))(?)
        verify(passportClient).createAnonymousSession()(?)
      }
  }

  test("get current session: successful") {
    val sid = ModelGenerators.SessionIdGen.next
    val expectedResult = ModelGenerators.sessionResultWithAutoruExpertGen().next

    when(passportClient.getSession(?)(?)).thenReturnF(expectedResult)

    Get(s"/1.0/session") ~>
      commonHeaders ~>
      addHeader("X-Session-Id", sid.value) ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        entityAs[SessionResult] shouldBe expectedResult
        verify(passportClient).getSession(eq(sid))(?)
      }
  }

  test("get current session: old url") {
    val sid = ModelGenerators.SessionIdGen.next
    val expectedResult = ModelGenerators.sessionResultWithAutoruExpertGen().next

    when(passportClient.getSession(?)(?)).thenReturnF(expectedResult)

    Get(s"/1.0/sessions/") ~>
      commonHeaders ~>
      addHeader("X-Session-Id", sid.value) ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        entityAs[SessionResult] shouldBe expectedResult
        verify(passportClient).getSession(eq(sid))(?)
      }
  }

}
