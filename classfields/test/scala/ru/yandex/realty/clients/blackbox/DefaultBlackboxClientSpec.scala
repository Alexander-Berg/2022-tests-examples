package ru.yandex.realty.clients.blackbox

import akka.http.scaladsl.model.HttpMethods
import com.typesafe.config.Config
import org.junit.runner.RunWith
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.{JsValue, Json}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.blackbox.BlackboxClient.UserInfoRequest
import ru.yandex.realty.clients.blackbox.DefaultBlackboxClientSpec.{errorStatusResponse, exceptionResponse}
import ru.yandex.realty.clients.blackbox.model.{OAuth, OAuthValidResponse, UserTicketResponse}
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.realty.pos.TestOperationalComponents
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.tvm.TvmLibrarySupplier
import ru.yandex.vertis.generators.BasicGenerators

@RunWith(classOf[JUnitRunner])
class DefaultBlackboxClientSpec
  extends AsyncSpecBase
  with PropertyChecks
  with RequestAware
  with HttpClientMock
  with BasicGenerators
  with ShrinkLowPriority
  with TvmLibrarySupplier
  with TestOperationalComponents {

  override protected def tvmConf: Option[Config] = None

  private val client = new DefaultBlackboxClient(httpService, tvmLibraryApi)

  val userIp = "127.0.0.1"
  implicit val traced: Traced = Traced.empty

  "DefaultBlackboxClient" when {
    "get userinfo" should {
      val requestGen: Gen[UserInfoRequest] =
        readableString.flatMap { id =>
          Gen.oneOf(UserInfoRequest.ByUid(id), UserInfoRequest.ByLogin(id))
        }
      def userInfoTest(test: UserInfoRequest => Unit): Unit = {
        val request = requestGen.next
        val idParam = request match {
          case UserInfoRequest.ByUid(uid) => s"uid=$uid"
          case UserInfoRequest.ByLogin(login) => s"login=$login"
        }
        httpClient.expect(
          HttpMethods.GET,
          s"/blackbox?method=userinfo&userip=$userIp&format=json&$idParam&" +
            s"getphones=bound&phone_attributes=102,103,104,107&attributes=1007&emails=getall&regname=yes"
        )

        test(request)
      }

      "fail on unexpected response" in {
        userInfoTest { request =>
          httpClient.respondWithJson("[]")
          interceptCause[IllegalArgumentException] {
            client.userInfo(request, userIp).futureValue
          }
        }
      }

      "fail on empty users array" in {
        userInfoTest { request =>
          httpClient.respondWithJsonFrom("/blackbox/userinfo_rs_success_empty_array.json")
          interceptCause[IllegalArgumentException] {
            client.userInfo(request, userIp).futureValue
          }
        }
      }

      "fail on empty user with id" in {
        userInfoTest { request =>
          httpClient.respondWithJsonFrom("/blackbox/userinfo_rs_success_empty_with_id.json")
          val response = client.userInfo(request).futureValue
          response match {
            case UserInfoUnknown("4001078984999") =>
            case _ =>
              fail(s"Unexpected response $response")
          }
        }
      }

      "fail on empty user without id" in {
        userInfoTest { request =>
          httpClient.respondWithJsonFrom("/blackbox/userinfo_rs_success_empty_no_id.json")
          val response = client.userInfo(request).futureValue
          response match {
            case UserInfoUnknown("") =>
            case _ =>
              fail(s"Unexpected response $response")
          }
        }
      }

      "fail on blackbox exception" in {
        checkExceptions { () =>
          client.userInfo(requestGen.next, userIp).futureValue
        }
      }

      "parse userinfo" in {
        userInfoTest { request =>
          httpClient.respondWithJsonFrom("/blackbox/userinfo_rs_success.json")
          val response = client.userInfo(request, userIp).futureValue
          response match {
            case UserInfoSuccess(userInfo) =>
              userInfo.uid shouldBe "4001078984"
              userInfo.login shouldBe "vsapps"
              userInfo.name should contain("Ver Ver")
              userInfo.defaultEmail.map(_.email) should contain("vsapps@yandex.ru")
              val expectedPhone = UserPhone("14295562", "+7977*****67", "+79773405067")
              userInfo.phones should (have size 1 and contain(expectedPhone))
              userInfo.displayName.flatMap(_.avatar).foreach(_.empty shouldBe false)
              userInfo.displayName.flatMap(_.avatar).map(_.id) should contain("4000217463")
              userInfo.displayName.flatMap(_.avatar).flatMap(_.sizes).map(_.islands34) should contain(
                s"${Avatar.avatarsUrl}/4000217463/islands-34"
              )
              userInfo.displayName.flatMap(_.social).map(_.profileId) should contain("5328")
              userInfo.displayName.flatMap(_.social).map(_.provider) should contain("tw")
              userInfo.displayName.flatMap(_.social).map(_.redirectTarget) should contain(
                "1323266014.26924.5328.9e5e3b502d5ee16abc40cf1d972a1c17"
              )

            case _ =>
              fail(s"Unexpected response $response")
          }
        }
      }

      "parse userinfo with no display_name" in {
        userInfoTest { request =>
          httpClient.respondWithJsonFrom("/blackbox/userinfo_rs_success_no_display_name.json")
          val response = client.userInfo(request, userIp).futureValue
          response match {
            case UserInfoSuccess(userInfo) =>
              userInfo.displayName shouldBe None

            case _ =>
              fail(s"Unexpected response $response")
          }
        }
      }
    }

    "check session" should {
      val sessionId = "sessionid"
      val sessionid2 = "sessionid2"
      val sslSessionId = Some(sessionid2)

      def checkSessionTest(test: => Unit): Unit = {
        httpClient.expect(
          HttpMethods.GET,
          s"/blackbox?method=sessionid&userip=$userIp&format=json&sessionid=$sessionId&" +
            s"host=yandex.ru&sslsessionid=$sessionid2&attributes=120&get_user_ticket=yes"
        )
        test
      }

      "fail on blackbox exception" in {
        checkExceptions { () =>
          client.checkSession(sessionId, sslSessionId, userIp).futureValue
        }
      }

      "fail on expired" in {
        checkSessionTest {
          httpClient.respondWithJson(errorStatusResponse(2))
          client.checkSession(sessionId, Some(sessionid2), userIp).futureValue shouldBe a[CheckSessionExpired]
        }
      }

      "fail on no auth" in {
        checkSessionTest {
          httpClient.respondWithJson(errorStatusResponse(3))
          client.checkSession(sessionId, Some(sessionid2), userIp).futureValue shouldBe a[CheckSessionNoAuth]
        }
      }

      "fail on disabled" in {
        checkSessionTest {
          httpClient.respondWithJson(errorStatusResponse(4))
          client.checkSession(sessionId, Some(sessionid2), userIp).futureValue shouldBe a[CheckSessionDisabled]

        }
      }

      "fail on invalid" in {
        checkSessionTest {
          httpClient.respondWithJson(errorStatusResponse(5))
          client.checkSession(sessionId, Some(sessionid2), userIp).futureValue shouldBe a[CheckSessionInvalid]
        }
      }

      "parse response" in {
        checkSessionTest {
          httpClient.respondWithJsonFrom("/blackbox/sessionid_rs_success.json")
          val response = client.checkSession(sessionId, Some(sessionid2), userIp).futureValue
          response match {
            case CheckSessionSuccess(authInfo) =>
              authInfo.uidOpt should contain("4001053214")
              authInfo.login should contain("marbya9")
              authInfo.karma shouldBe 1
              authInfo.karmaStatus shouldBe 2
            case _ =>
              fail(s"Unexpected response $response")
          }
        }
      }
    }

    "check oauth" should {
      val token = "token"
      val deviceUuid = "device"

      def checkOauthTest(test: => Unit): Unit = {
        httpClient.expect(
          HttpMethods.GET,
          s"/blackbox?method=oauth&userip=$userIp&format=json&oauth_token=$token&attributes=137,138,73&get_user_ticket=yes"
        )
        test
      }

      "fail on blackbox exception" in {
        checkExceptions { () =>
          client.checkOauth(token, Some(deviceUuid), userIp).futureValue
        }
      }

      "fail on disabled" in {
        checkOauthTest {
          httpClient.respondWithJson(errorStatusResponse(4))
          client.checkOauth(token, Some(deviceUuid), userIp).futureValue shouldBe a[CheckOAuthDisabled]
        }
      }

      "fail on invalid" in {
        checkOauthTest {
          httpClient.respondWithJson(errorStatusResponse(5))
          client.checkOauth(token, Some(deviceUuid), userIp).futureValue shouldBe a[CheckOAuthInvalid]
        }
      }

      "parse response" in {
        checkOauthTest {
          httpClient.respondWithJsonFrom("/blackbox/oauth_rs_success.json")
          val response = client.checkOauth(token, Some(deviceUuid), userIp).futureValue
          response match {
            case CheckOAuthSuccess(authInfo) =>
              authInfo.uidOpt should contain("4013231548")
              authInfo.uuid should contain(deviceUuid)
              authInfo.login should contain("ana")
              authInfo.token should contain(token)
              (authInfo.scope should contain).allOf("mobile:all", "social:broker", "amsample:pay")
              Seq("android", "ios").foreach { platform =>
                authInfo.isHaveSid(platform) shouldBe true
              }
              authInfo.isHaveSid("desktop") shouldBe false
              authInfo.karma shouldBe 1
              authInfo.karmaStatus shouldBe 2
            case _ =>
              fail(s"Unexpected response $response")
          }
        }
      }
    }

    "oauth" should {
      "return successful response" in new Data {
        oauthTest {
          httpClient.respondWithJson(sampleOauthResponseJsonStr)
          val result = client.oauth(sampleToken, userIp, Map("get_user_ticket" -> "yes")).futureValue

          result match {
            case response: OAuthValidResponse =>
              response shouldEqual (sampleOauthResponseJson.as[OAuthValidResponse])

              // details
              response.oauth shouldEqual (sampleOauthResponseJson \ "oauth").as[OAuth]
              response.oauth.uid shouldEqual (sampleOauthResponseJson \ "oauth" \ "uid").as[String]
              response.userTicket shouldEqual (sampleOauthResponseJson \ "user_ticket").as[String]
            case _ =>
              fail(s"Unexpected response $result")
          }
        }
      }
    }

    "userTicket" should {
      "return userTicket response" in new Data {
        val responseJson: String = """ {
                         |    "users": [
                         |        {
                         |            "id": "123",
                         |            "uid": {
                         |                "value": "123",
                         |                "lite": false,
                         |                "hosted": false
                         |            },
                         |            "login": "drvr10",
                         |            "have_password": true,
                         |            "have_hint": true,
                         |            "karma": {
                         |                "value": 0
                         |            },
                         |            "karma_status": {
                         |                "value": 0
                         |            }
                         |        }
                         |    ]
                         |}""".stripMargin
        httpClient.respondWithJson(responseJson)

        val userTicketResponse: UserTicketResponse = client.userTicket("ticket", Map()).futureValue
        val uidOpt: Option[Long] =
          userTicketResponse.users.headOption.map(user => user.uid.value).map(value => value.toLong)
        uidOpt should not be empty
        uidOpt.get shouldBe 123L
      }
    }
  }

  def checkExceptions(runCall: () => Unit): Unit = {
    BlackboxClientException.ExceptionCodes.values.foreach { code =>
      httpClient.respondWithJson(exceptionResponse(code))
      val e = interceptCause[BlackboxClientException] {
        runCall()
      }
      e.code shouldBe code
    }
  }

  trait Data {
    val sampleToken: String = readableString.next
    val sampleParams: Map[String, String] = Map.empty

    def oauthTest(test: => Unit): Unit = {
      httpClient.expect(
        HttpMethods.GET,
        s"/blackbox?method=oauth&userip=$userIp&format=json&oauth_token=$sampleToken&get_user_ticket=yes"
      )
      test
    }

    val sampleOauthResponseJsonStr: String =
      s"""
         |{
         |  "oauth": {
         |    "uid": "${posNum[Long].next}",
         |    "token_id": "${posNum[Long].next}",
         |    "device_id": "${readableString.next}",
         |    "device_name": "${readableString.next}",
         |    "scope": "${readableString.next}:use ${readableString.next}:all",
         |    "ctime": "2021-10-19 15:21:36",
         |    "issue_time": "2021-10-19 15:21:36",
         |    "expire_time": ${bool.next},
         |    "is_ttl_refreshable": ${bool.next},
         |    "client_id": "${readableString.next}",
         |    "client_name": "${readableString.next}",
         |    "client_icon": "",
         |    "client_homepage": "https:\\/\\/${readableString.next}.realty.test.vertis.yandex.ru\\/",
         |    "client_ctime": "2021-10-19 13:44:52",
         |    "client_is_yandex": ${bool.next},
         |    "xtoken_id": "aa",
         |    "meta": "bb"
         |  },
         |  "status": {
         |    "value": "VALID",
         |    "id": 0
         |  },
         |  "error": "OK",
         |  "uid": {
         |    "value": "${posNum[Long].next}",
         |    "lite": ${bool.next},
         |    "hosted": ${bool.next}
         |  },
         |  "login": "${readableString.next}",
         |  "have_password": ${bool.next},
         |  "have_hint": ${bool.next},
         |  "karma": {
         |    "value": ${posNum[Int].next}
         |  },
         |  "karma_status": {
         |    "value": ${posNum[Int].next}
         |  },
         |  "user_ticket": "${posNum[Int].next}:${readableString.next}:${readableString.next}",
         |  "connection_id": "t:${posNum[Int].next}"
         |}
         |
         |""".stripMargin

    val sampleOauthResponseJson: JsValue = Json.parse(sampleOauthResponseJsonStr)
  }
}

object DefaultBlackboxClientSpec {

  def errorStatusResponse(code: Int): String =
    s"""
       |{
       |  "status" : {
       |    "id" : $code,
       |    "value" : "VALID"
       |  }
       |}
     """.stripMargin

  def exceptionResponse(code: BlackboxClientException.ExceptionCodes.Value): String =
    s"""
       |{
       |  "exception":
       |  {
       |    "value":"${code.toString}",
       |    "id":${code.id}
       |  },
       |  "error":"BlackBox error: Missing userip argument"
       |}
     """.stripMargin
}
