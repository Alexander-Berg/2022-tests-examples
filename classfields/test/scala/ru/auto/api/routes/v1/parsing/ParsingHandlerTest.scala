package ru.auto.api.routes.v1.parsing

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSuite
import ru.auto.api.ResponseModel.{DraftResponse, LoginResponse, ParsedOfferInfoResponse, SuccessResponse}
import ru.auto.api.managers.parsing.ParsingManager
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.{MockedClients, MockedPassport}
import ru.auto.api.util.StringUtils.RichString
import scala.concurrent.Future

/**
  * Created by andrey on 11/30/17.
  */
class ParsingHandlerTest extends ApiSuite with ScalaCheckPropertyChecks with MockedClients with MockedPassport {
  override lazy val parsingManager: ParsingManager = mock[ParsingManager]

  before {
    reset(passportManager)
  }

  after {
    verifyNoMoreInteractions(passportManager)
    verifyNoMoreInteractions(parsingManager)
  }

  test("create draft from hash") {
    val hash = "hash1"
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
    when(parsingManager.createDraftFromParsedOffer(?, ?, ?)(?))
      .thenReturn(Future.successful(DraftResponse.getDefaultInstance))
    val user = ModelGenerators.PrivateUserRefGen.next
    Post(s"/1.0/parsing/$hash/to-draft") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(parsingManager).createDraftFromParsedOffer(eq(user), eq(hash), eq(None))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("create draft from url") {
    val url = "https://m.avito.ru/penza/avtomobili/ssangyong_korando_1997_440422701"
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
    when(parsingManager.createDraftFromParsedOfferByUrl(?, ?)(?))
      .thenReturn(Future.successful(DraftResponse.getDefaultInstance))
    val user = ModelGenerators.PrivateUserRefGen.next
    Post(s"/1.0/parsing/create-draft?url=${url.escaped}") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(parsingManager).createDraftFromParsedOfferByUrl(eq(user), eq(url))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("create draft from hash with custom phone") {
    val hash = "hash1"
    val phone = "79297776688"
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
    when(parsingManager.createDraftFromParsedOffer(?, ?, ?)(?))
      .thenReturn(Future.successful(DraftResponse.getDefaultInstance))
    val user = ModelGenerators.PrivateUserRefGen.next
    Post(s"/1.0/parsing/$hash/to-draft?phone=$phone") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(parsingManager).createDraftFromParsedOffer(eq(user), eq(hash), eq(Some(phone)))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("create draft from hash without auth") {
    val hash = "hash1"
    when(passportManager.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
    when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
    Post(s"/1.0/parsing/$hash/to-draft") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.Unauthorized
          responseAs[String] should matchJson("""{
              |  "error": "NO_AUTH",
              |  "status": "ERROR",
              |  "detailed_error": "Need authentication"
              |}""".stripMargin)
        }
      }
    verifyNoMoreInteractions(parsingManager)
    verify(passportManager).createAnonymousSession()(?)
    verify(passportManager).getSessionFromUserTicket()(?)
  }

  test("parsed offer info request") {
    val hash = "hash1"
    when(passportManager.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
    when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
    when(parsingManager.getParsedOfferInfo(?)(?))
      .thenReturn(Future.successful(ParsedOfferInfoResponse.getDefaultInstance))
    Get(s"/1.0/parsing/$hash/info") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(parsingManager).getParsedOfferInfo(eq(hash))(?)
    verify(passportManager).createAnonymousSession()(?)
    verify(passportManager).getSessionFromUserTicket()(?)
  }

  test("set not_published") {
    val hash = "hash1"
    val reason = "noanswer"
    when(passportManager.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
    when(parsingManager.setRejectReason(?, ?)(?)).thenReturnF(SuccessResponse.getDefaultInstance)
    when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
    Put(s"/1.0/parsing/$hash/reject-reason?reason=$reason") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(parsingManager).setRejectReason(eq(hash), eq(reason))(?)
    verify(passportManager).createAnonymousSession()(?)
    verify(passportManager).getSessionFromUserTicket()(?)
  }

  test("login-or-register") {
    val hash = "hash1"
    when(passportManager.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
    when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
    when(parsingManager.loginOrRegisterOwner(?, ?)(?)).thenReturnF(LoginResponse.getDefaultInstance)
    Put(s"/1.0/parsing/$hash/login-or-register-owner") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(parsingManager).loginOrRegisterOwner(eq(hash), eq(None))(?)
    verify(passportManager).createAnonymousSession()(?)
    verify(passportManager).getSessionFromUserTicket()(?)
  }

  test("login-or-register with custom phone") {
    val hash = "hash1"
    val phone = "79295556677"
    when(passportManager.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
    when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
    when(parsingManager.loginOrRegisterOwner(?, ?)(?)).thenReturnF(LoginResponse.getDefaultInstance)
    Put(s"/1.0/parsing/$hash/login-or-register-owner?phone=$phone") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(parsingManager).loginOrRegisterOwner(eq(hash), eq(Some(phone)))(?)
    verify(passportManager).createAnonymousSession()(?)
    verify(passportManager).getSessionFromUserTicket()(?)
  }
}
