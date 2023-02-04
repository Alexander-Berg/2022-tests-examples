package ru.yandex.vertis.passport.service.user.social.clients

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, BasicHttpCredentials, OAuth2BearerToken}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.{AsyncWordSpec, PrivateMethodTester}
import pureconfig.loadConfigOrThrow
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel.SocialUserPayload
import ru.yandex.vertis.passport.AkkaSupport
import ru.yandex.vertis.passport.model.{SocialUserPhone, SocialUserSource}
import ru.yandex.vertis.passport.service.user.social.clients.SimpleOAuth2Client.OAuthAuthorizationException
import ru.yandex.vertis.passport.service.user.social.{MosRuConfig, MosRuEndpoints, MosRuMobileOAuth2Config, StandardOAuth2Config}
import ru.yandex.vertis.passport.test.SpecBase
import ru.yandex.vertis.passport.util.http.HttpClientMock
import ru.yandex.vertis.passport.util.lang.RichString

class MosRuClientSpec
  extends AsyncWordSpec
  with SpecBase
  with AkkaSupport
  with Eventually
  with HttpClientMock
  with PrivateMethodTester {

  "mos.ru config" should {
    "be parsed from file" in withConfig { cfg =>
      val tsConfig = ConfigFactory.parseString("""
          |# datasources.passport.social-providers.mosru
          | web = {
          |   client-id = "auto.ru"
          |   client-secret = "test_secret"
          |   redirect-uri = "https://redirect.io"
          | }
          |
          | mobile = {
          |   software-id = "auto.ru"
          |   initial-access-token = "test"
          |   software-statement = "test"
          | }
          | 
          | endpoints = {
          |   issuer = "https://login-tech.mos.ru",
          |   authorization = "https://login-tech.mos.ru/sps/oauth/ae"
          |   token = "https://login-tech.mos.ru/sps/oauth/te"
          |   user-info = "https://login-tech.mos.ru/sps/oauth/me"
          |   registration = "https://login-tech.mos.ru/sps/oauth/register"
          |   jwks-uri = "https://login-tech.mos.ru/sps/oauth/.well-known/jwks"
          |   introspect = "https://login-tech.mos.ru/sps/oauth/introspect"
          | }
          | scope = "openid profile contacts depers"
          |
          |""".stripMargin)
      loadConfigOrThrow[MosRuConfig](tsConfig) should be(cfg)
    }
  }

  "mos.ru client" should {

    "create auth link" in withDefaults { (cfg, client) =>
      val state = "iddqd"
      val authorizationUri = client.authorizationUri(state, ApiModel.Platform.DESKTOP, None)
      val expectedUrl = "https://login-tech.mos.ru/sps/oauth/ae" +
        "?state=iddqd" +
        "&scope=openid+profile+contacts+depers" +
        "&redirect_uri=https://redirect.io" +
        "&client_id=auto.ru" +
        "&response_type=code"

      assert(authorizationUri == expectedUrl)
    }

    "get access_token" in withDefaults { (cfg, client) =>
      val code = "iddqd"
      val expectedHeaders: Seq[HttpHeader] = Seq(
        Accept(MediaTypes.`application/json`),
        Authorization(BasicHttpCredentials(cfg.web.clientId, cfg.web.clientSecret))
      )
      val expectedUri = s"${cfg.endpoints.token}" +
        s"?grant_type=authorization_code" +
        s"&code=$code" +
        s"&redirect_uri=${cfg.web.redirectUri.get}"

      val response =
        """{
          |    "id_token": "test_id_token",
          |    "access_token": "test_access_token",
          |    "expires_in": 3600,
          |    "scope": "openid profile contacts depers",
          |    "token_type": "Bearer"
          |}""".stripMargin

      onRequest { req =>
        req.method should be(HttpMethods.POST)
        req.uri.toString should be(expectedUri)
        req.headers should contain theSameElementsAs (expectedHeaders)
        req.entity.contentType should be(ContentType(MediaTypes.`application/json`))

        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentType(MediaTypes.`application/json`),
            response.getBytes
          )
        )
      }

      val token = client.getToken(code)
      token.map { res =>
        res.accessToken should be("test_access_token")
      }
    }

    "get user info by token" in withDefaults { (cfg, client) =>
      val token = "idkfa"
      val expectedHeaders = Seq(Accept(MediaTypes.`application/json`), Authorization(OAuth2BearerToken(token)))
      val payload = SocialUserPayload.newBuilder()
      payload.getMosruBuilder.getSnilsExistsBuilder.setValue(false)
      val expectedSocialUser = SocialUserSource(
        id = "test_guid",
        emails = Seq("test@test.com"),
        phones = Seq(SocialUserPhone("test_phone")),
        firstName = Option("fTest"),
        lastName = Option("lTest"),
        payload = payload.build()
      )

      val response =
        """{
          |    "LastName": "lTest",
          |    "MiddleName": "mTest",
          |    "guid": "test_guid",
          |    "FirstName": "fTest",
          |    "mail": "test@test.com",
          |    "mobile": "test_phone"
          |}
          |""".stripMargin

      onRequest { req =>
        req.method should be(HttpMethods.GET)
        req.uri.toString should be(cfg.endpoints.userInfo)
        req.headers should contain theSameElementsAs (expectedHeaders)
        req.entity.contentType should be(ContentTypes.NoContentType)

        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentType(MediaTypes.`application/json`),
            response.getBytes
          )
        )
      }

      client.getUserByToken(token).map { user =>
        user should be(expectedSocialUser)
      }
    }

    "get user info with snils_exists by token" in withDefaults { (cfg, client) =>
      val token = "idkfa"
      val expectedHeaders = Seq(Accept(MediaTypes.`application/json`), Authorization(OAuth2BearerToken(token)))
      val payload = SocialUserPayload.newBuilder()
      payload.getMosruBuilder.getSnilsExistsBuilder.setValue(true)
      val expectedSocialUser = SocialUserSource(
        id = "test_guid",
        emails = Seq("test@test.com"),
        phones = Seq(SocialUserPhone("test_phone")),
        firstName = Option("fTest"),
        lastName = Option("lTest"),
        payload = payload.build()
      )

      val response =
        """{
          |    "LastName": "lTest",
          |    "MiddleName": "mTest",
          |    "guid": "test_guid",
          |    "FirstName": "fTest",
          |    "mail": "test@test.com",
          |    "mobile": "test_phone",
          |    "snils_exists": true
          |}
          |""".stripMargin

      onRequest { req =>
        req.method should be(HttpMethods.GET)
        req.uri.toString should be(cfg.endpoints.userInfo)
        req.headers should contain theSameElementsAs (expectedHeaders)
        req.entity.contentType should be(ContentTypes.NoContentType)

        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentType(MediaTypes.`application/json`),
            response.getBytes
          )
        )
      }

      client.getUserByToken(token).map { user =>
        user should be(expectedSocialUser)
      }
    }

    "get user info with trusted and snils_exists by token" in withDefaults { (cfg, client) =>
      val token = "idkfa"
      val expectedHeaders = Seq(Accept(MediaTypes.`application/json`), Authorization(OAuth2BearerToken(token)))
      val payload = SocialUserPayload.newBuilder()
      payload.getMosruBuilder.getSnilsExistsBuilder.setValue(true)
      val expectedSocialUser = SocialUserSource(
        id = "test_guid",
        emails = Seq("test@test.com"),
        phones = Seq(SocialUserPhone("test_phone")),
        firstName = Option("fTest"),
        lastName = Option("lTest"),
        trusted = true,
        payload = payload.build()
      )

      val response =
        """{
          |    "LastName": "lTest",
          |    "MiddleName": "mTest",
          |    "guid": "test_guid",
          |    "FirstName": "fTest",
          |    "mail": "test@test.com",
          |    "mobile": "test_phone",
          |    "trusted": true,
          |    "snils_exists": true
          |}
          |""".stripMargin

      onRequest { req =>
        req.method should be(HttpMethods.GET)
        req.uri.toString should be(cfg.endpoints.userInfo)
        req.headers should contain theSameElementsAs (expectedHeaders)
        req.entity.contentType should be(ContentTypes.NoContentType)

        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentType(MediaTypes.`application/json`),
            response.getBytes
          )
        )
      }

      client.getUserByToken(token).map { user =>
        user should be(expectedSocialUser)
      }
    }

    "validate mobile token with active session" in withDefaults { (cfg, client) =>
      val userId = "doom_guy"

      val mobileClientId = s"dyn:${cfg.mobile.softwareId}:test"
      val token = "iddqd"

      val response =
        s"""
           |{
           |    "client_id": "$mobileClientId",
           |    "token_type": "Bearer",
           |    "active": true,
           |    "scope": "openid profile contacts",
           |    "jti": "test",
           |    "sbj": "$userId"
           |}
           |""".stripMargin

      onRequest { req =>
        req.method should be(HttpMethods.POST)
        req.uri.toString should be(cfg.endpoints.introspect)
        req.entity.contentType should be(ContentType(MediaTypes.`application/x-www-form-urlencoded`))

        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentType(MediaTypes.`application/json`),
            response.getBytes
          )
        )
      }

      client.validateToken(token).map {
        _ should be(userId)
      }
    }

    "drop mobile token with expired session" in withDefaults { (cfg, client) =>
      val response = HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentType(MediaTypes.`application/json`),
          "{\"active\":false}".getBytes
        )
      )

      onRequest { _ =>
        response
      }
      whenReady(client.validateToken("test").failed) { e =>
        e shouldBe an[OAuthAuthorizationException]
        e.getMessage should include("has no active sessions")
      }
    }

    "drop mobile token with wrong client id" in withDefaults { (cfg, client) =>
      val response = HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentType(MediaTypes.`application/json`),
          s"""
           |{
           |    "client_id": "din:auto.ru:test",
           |    "token_type": "Bearer",
           |    "active": true,
           |    "scope": "openid profile contacts",
           |    "jti": "test",
           |    "sbj": "test"
           |}
           |""".stripMargin.getBytes
        )
      )

      onRequest { _ =>
        response
      }
      whenReady(client.validateToken("test").failed) { e =>
        e shouldBe an[OAuthAuthorizationException]
        e.getMessage should include("is created for another application")
      }
    }

    "mangle sensitive data" in withDefaults { (cfg, client) =>

      "dO-xymwduYR8uFqvYYK1ghpk-tqantG5PstfomddlO5d2-BVzVVaNdHuJYdWxpL__c8MsLWB8IwmiUIEep53tnZR2mflAnYiguE0UyUZNBE".masked should equal(
        "dO-xymwduYR8uFqvYYK1g**************************************************************************************"
      )

      "".masked should equal("")
      "a".masked should equal("*")
      "ab".masked should equal("a*")
      "abc".masked should equal("a**")
      "abcd".masked should equal("a***")
      "abcde".masked should equal("a****")
    }
  }

  def withConfig[T](code: MosRuConfig => T): T =
    code(
      MosRuConfig(
        web = StandardOAuth2Config(
          clientId = "auto.ru",
          clientSecret = "test_secret",
          redirectUri = Option("https://redirect.io")
        ),
        mobile =
          MosRuMobileOAuth2Config(softwareId = "auto.ru", initialAccessToken = "test", softwareStatement = "test"),
        endpoints = MosRuEndpoints(
          issuer = "https://login-tech.mos.ru",
          authorization = "https://login-tech.mos.ru/sps/oauth/ae",
          token = "https://login-tech.mos.ru/sps/oauth/te",
          userInfo = "https://login-tech.mos.ru/sps/oauth/me",
          registration = "https://login-tech.mos.ru/sps/oauth/register",
          jwksUri = "https://login-tech.mos.ru/sps/oauth/.well-known/jwks",
          introspect = "https://login-tech.mos.ru/sps/oauth/introspect"
        ),
        scope = "openid profile contacts depers"
      )
    )

  def withClient[T](cfg: MosRuConfig)(code: MosRuClient => T): T = code(new MosRuClient(cfg, http))

  def withDefaults[T](code: (MosRuConfig, MosRuClient) => T): T = withConfig { cfg =>
    withClient(cfg) { client =>
      code(cfg, client)
    }
  }
}
