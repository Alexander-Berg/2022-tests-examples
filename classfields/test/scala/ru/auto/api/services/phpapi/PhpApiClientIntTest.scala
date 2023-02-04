package ru.auto.api.services.phpapi

import java.io.Closeable

import org.scalacheck.Gen
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import ru.auto.api.auth.Application
import ru.auto.api.http.{HttpClient, HttpClientConfig}
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.comments.TopicGroup._
import ru.auto.api.model.{Paging, RequestParams, UserRef}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.passport.DefaultPassportClient
import ru.auto.api.services.phpapi.model.PhpComment
import ru.auto.api.util.{Logging, Request, RequestImpl}
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel._
import ru.yandex.vertis.tracing.Traced

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 30/10/2017.
  */
@Ignore
class PhpApiClientIntTest extends HttpClientSuite with Matchers with ScalaFutures with TestRequest with Logging {

  override protected def config: HttpClientConfig = {
    HttpClientConfig("https", "api.test.avto.ru", 443)
  }

  val client = new DefaultPhpApiClient(http)

  val passportConfig: HttpClientConfig = {
    HttpClientConfig("http", "passport-api-01-sas.test.vertis.yandex.net", 6210)
  }

  private val passportHttp: HttpClient with Closeable =
    HttpClientSuite.createClientForTests(passportConfig, cachingProxy)
  private val passportClient = new DefaultPassportClient(passportHttp)

  private val email = "test@auto.ru"

  private val loginParams = LoginParameters
    .newBuilder()
    .setLogin(email)
    .setPassword("autoru")
    .build()

  private lazy val loginRes: ApiModel.LoginResult = passportClient.login(loginParams).futureValue
  private lazy val session: ApiModel.Session = loginRes.getSession

  lazy val requestWithSession: Request = {
    val r = new RequestImpl
    r.setTrace(Traced.empty)
    r.setRequestParams(RequestParams.construct("192.0.0.1", sessionId = Some(session.getId)))
    r.setApplication(Application.swagger)
    r.setUser(UserRef.user(1))
    r.setSession(SessionResult.newBuilder().setSession(session).build())
    r.setNewDeviceUid("uid")
    r
  }

  private val pagination1 = Paging(1, 1)
  private val pagination2 = Paging(2, 1)

  private val reviewId = Gen.posNum[Int].next.toString
  private var commentId = Gen.posNum[Int].next.toString

  test("add and get comment") {
    val addResponse1: PhpComment =
      client.addComment(REVIEWS, reviewId, "test message1", 0, limitOff = true)(requestWithSession).futureValue
    val addResponse2: PhpComment =
      client.addComment(REVIEWS, reviewId, "test message2", 0, limitOff = true)(requestWithSession).futureValue

    val getResponse1 = client.getComments(REVIEWS, reviewId, pagination1)(requestWithSession).futureValue
    val getComment1 = getResponse1.comments.head

    val getResponse2 = client.getComments(REVIEWS, reviewId, pagination2)(requestWithSession).futureValue
    val getComment2 = getResponse2.comments.head

    getComment1.id shouldBe addResponse2.id
    getComment2.id shouldBe addResponse1.id

    commentId = getComment1.id

  }

  test("complain on comment") {
    client.complainOnComment(commentId, "test message1", limitOff = true)(requestWithSession).futureValue
  }

  test("delete comment") {
    client.deleteComment(commentId)(requestWithSession).futureValue

    val getResponse1 = client.getComments(REVIEWS, reviewId, pagination1)(requestWithSession).futureValue
    getResponse1.comments.head.hidden shouldBe "1"
  }

}
