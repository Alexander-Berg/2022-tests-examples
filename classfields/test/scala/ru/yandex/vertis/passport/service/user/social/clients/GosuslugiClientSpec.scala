package ru.yandex.vertis.passport.service.user.social.clients

import java.security.{SecureRandom, Security}
import java.util.{Base64, Date}

import akka.http.scaladsl.model._
import com.typesafe.config.ConfigFactory
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.{SubjectPublicKeyInfo, Time}
import org.bouncycastle.cert.X509v1CertificateBuilder
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import org.bouncycastle.operator.{ContentSigner, DefaultDigestAlgorithmIdentifierFinder, DefaultSignatureAlgorithmIdentifierFinder}
import org.scalatest.concurrent.Eventually
import org.scalatest.{AsyncWordSpec, PrivateMethodTester}
import play.api.libs.json.Json
import pureconfig.loadConfigOrThrow
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel.SocialUserPayload
import ru.yandex.vertis.mockito.MockitoSupport._
import ru.yandex.vertis.passport.AkkaSupport
import ru.yandex.vertis.passport.integration.yav.YavClient
import ru.yandex.vertis.passport.model.SocialUserSource
import ru.yandex.vertis.passport.service.user.social.clients.Gosuslugi.GosuslugiToken.gosuslugiParsedIdTokenReads
import ru.yandex.vertis.passport.service.user.social.clients.Gosuslugi.{GosuslugiSigner, GosuslugiToken}
import ru.yandex.vertis.passport.service.user.social.{GosuslugiConfig, GosuslugiEndpoints, GosuslugiSecrets, StandardOAuth2Config}
import ru.yandex.vertis.passport.test.SpecBase
import ru.yandex.vertis.passport.util.http.HttpClientResuableMock
import ru.yandex.vertis.passport.util.lang.RichString

import scala.concurrent.Future

class GosuslugiClientSpec
  extends AsyncWordSpec
  with SpecBase
  with AkkaSupport
  with Eventually
  with HttpClientResuableMock
  with PrivateMethodTester {
  Security.addProvider(new BouncyCastleProvider)

  "gosuslugi config" should {
    "be parsed from file" in withConfig { cfg =>

      val tsConfig = ConfigFactory.parseString(
        """
                                                 |# datasources.passport.social-providers.gosuslugi
                                                 | standard-config.client-id = "AUTORU"
                                                 | standard-config.client-secret = ""
                                                 | standard-config.redirect-uri = "https://auth.test.avto.ru/social/callback/gosuslugi/"
                                                 | endpoints.authorization = "https://esia-portal1.test.gosuslugi.ru/aas/oauth2/ac"
                                                 | endpoints.token =  "https://esia-portal1.test.gosuslugi.ru/aas/oauth2/te"
                                                 | endpoints.user-info =  "https://esia-portal1.test.gosuslugi.ru/rs/prns"
                                                 | endpoints.registration =  "https://esia-portal1.test.gosuslugi.ru/registration/"
                                                 | secrets.certificate-secret-name = "certificate-test"
                                                 | secrets.private-key-secret-name = "privatekey-test"
                                                 | secrets.secret-uid = "sec-01ehcc0633yvwmqa1fz1fvmd66"
                                                 |
                                                 | scope = "openid profile contacts depers"
                                                 |
                                                 |""".stripMargin
      )
      loadConfigOrThrow[GosuslugiConfig](tsConfig) should be(cfg)
    }
  }

  "gosuslugi client" should {

    "create auth link" in withDefaults { (cfg, client) =>
      val authorizationUri = client.authorizationUri("", ApiModel.Platform.DESKTOP, None)
      val expectedUrl = "https://esia-portal1.test.gosuslugi.ru/aas/oauth2/ac" +
        "?state=iddqd" +
        "&scope=openid+profile+contacts+depers" +
        "&redirect_uri=https://redirect.io" +
        "&client_id=AUTORU" +
        "&response_type=code"
      assert(
        authorizationUri.contains("https://esia-portal1.test.gosuslugi.ru/aas/oauth2/ac") &&
          authorizationUri.contains("&scope=openid+profile+contacts+depers") &&
          authorizationUri.contains("&redirect_uri=https://auth.test.avto.ru/social/callback/gosuslugi/") &&
          authorizationUri.contains("&response_type=code")
      )
    }

    "get access_token" in withDefaults { (cfg, client) =>
      val code = "iddqd"
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
        req.headers should be(Nil)
        req.entity.contentType should be(ContentType(MediaTypes.`application/x-www-form-urlencoded`))

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
      val token =
        "aa.eyJhdWQiOiJBVVRPUlUiLCJzdWIiOjI0NjAxMjEwMSwibmJmIjoxNjM3MjU5MTc0LCJhbXIiOiJQV0QiLCJ1cm46ZXNpYTphbWQiOiJQV0QiLCJhdXRoX3RpbWUiOjE2MzcyNTQ3MDAsImlzcyI6Imh0dHA6XC9cL2VzaWEuZ29zdXNsdWdpLnJ1XC8iLCJ1cm46ZXNpYTpzaWQiOiIxMmI1YjVlNC1hNTVmLTRjMjEtYTFlNC1jZTdhZjAwYTg1YmUiLCJ1cm46ZXNpYTpzYmoiOnsidXJuOmVzaWE6c2JqOnR5cCI6IlAiLCJ1cm46ZXNpYTpzYmo6aXNfdHJ1Ijp0cnVlLCJ1cm46ZXNpYTpzYmo6b2lkIjoyNDYwMTIxMDEsInVybjplc2lhOnNiajpuYW0iOiIxNTQtNTQwLTk0MCA2MSJ9LCJleHAiOjE2MzcyNjk5NzQsImlhdCI6MTYzNzI1OTE3NH0.bb"

      val parsedToken = GosuslugiToken.parseToJson(token).as[Gosuslugi.GosuslugiIdToken].oid.toString
      val payload = SocialUserPayload.newBuilder()
      payload.getMosruBuilder.getSnilsExistsBuilder.setValue(false)
      val expectedSocialUser = SocialUserSource(
        id = "246012101",
        emails = Nil,
        phones = Seq(),
        firstName = None,
        lastName = None,
        trusted = true
      )

      val response =
        """{
          |    "lastName": "lTest",
          |    "middleName": "mTest",
          |    "firstName": "fTest",
          |    "trusted": true
          |}
          |""".stripMargin

      val responseCtts =
        """{
          |    "elements": [
          |    "https://esia-portal1.test.gosuslugi.ru/rs/prns/1000299706/ctts/12345",
          |    "https://esia-portal1.test.gosuslugi.ru/rs/prns/1000299706/ctts/23456"
          |    ]
          |    }
          |""".stripMargin

      val responseContactPhone =
        """
          |    {"stateFacts":["Identifiable"],"id":14338940,"type":"MBT","vrfStu":"VERIFIED","value":"+7(977)8318309","eTag":"3411F9B15D1FB2ECE29B121A02C1C2A9451A5375"}
          |""".stripMargin

      val responseContactMail =
        """
          |    {"stateFacts":["Identifiable"],"id":14432744,"type":"EML","vrfStu":"NOT_VERIFIED","value":"EsiaTest006@yandex.ru","verifyingValue":"EsiaTest006@yandex.ru","vrfValStu":"VERIFYING","isCfmCodeExpired":false,"eTag":"A64BD73DA59E465A9846BD5334417659FC7A7AF8"}
          |""".stripMargin

      onRequest {
        case HttpRequest(HttpMethods.GET, uri, _, entity, _) if uri.toString().contains("/ctts/12345") =>
          uri.toString should be(cfg.endpoints.userInfo + s"/$parsedToken/ctts/12345")
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentType(MediaTypes.`application/json`),
              responseContactPhone.getBytes
            )
          )
        case HttpRequest(HttpMethods.GET, uri, _, entity, _) if uri.toString().contains("/ctts/23456") =>
          uri.toString should be(cfg.endpoints.userInfo + s"/$parsedToken/ctts/23456")
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentType(MediaTypes.`application/json`),
              responseContactMail.getBytes
            )
          )
        case HttpRequest(HttpMethods.GET, uri, _, entity, _) if uri.toString().contains("/ctts") =>
          uri.toString should be(cfg.endpoints.userInfo + s"/$parsedToken/ctts")
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentType(MediaTypes.`application/json`),
              responseCtts.getBytes
            )
          )
        case HttpRequest(HttpMethods.GET, uri, _, entity, _) =>
          uri.toString should be(cfg.endpoints.userInfo + s"/$parsedToken")
          entity.contentType should be(ContentTypes.NoContentType)
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

  def withConfig[T](code: GosuslugiConfig => T): T =
    code(
      GosuslugiConfig(
        standardConfig = StandardOAuth2Config(
          clientId = "AUTORU",
          clientSecret = "",
          redirectUri = Option("https://auth.test.avto.ru/social/callback/gosuslugi/")
        ),
        endpoints = GosuslugiEndpoints(
          authorization = "https://esia-portal1.test.gosuslugi.ru/aas/oauth2/ac",
          token = "https://esia-portal1.test.gosuslugi.ru/aas/oauth2/te",
          userInfo = "https://esia-portal1.test.gosuslugi.ru/rs/prns"
        ),
        scope = "openid profile contacts depers",
        secrets = GosuslugiSecrets(
          certificateSecretName = "certificate-test",
          privateKeySecretName = "privatekey-test",
          secretUID = "sec-01ehcc0633yvwmqa1fz1fvmd66"
        )
      )
    )

  def withClient[T](cfg: GosuslugiConfig)(code: GosuslugiClient => T): T = {
    import java.math.BigInteger
    import java.security.KeyPairGenerator

    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048, new SecureRandom())
    val keyPair = keyPairGenerator.genKeyPair()
    val dnName = new X500Name("CN=Yandex team")
    val subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic.getEncoded)
    val certGen = new X509v1CertificateBuilder(
      dnName,
      BigInteger.valueOf(System.currentTimeMillis),
      new Time(new Date()),
      new Time(new Date(System.currentTimeMillis() * 2)),
      dnName,
      subPubKeyInfo
    )
    val sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA")
    val digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
    val privateKey = keyPair.getPrivate
    val contentSigner: ContentSigner =
      new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
        .build(PrivateKeyFactory.createKey(privateKey.getEncoded))
    val cert = certGen.build(contentSigner)
    val pkInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded)
    val encodable = pkInfo.parsePrivateKey
    val primitive = encodable.toASN1Primitive
    val privateKeyPKCS1 = primitive.getEncoded
    val keyEncoded = {
      "-----BEGIN RSA PRIVATE KEY-----\n" +
        Base64.getEncoder.encodeToString(privateKeyPKCS1) +
        "\n-----END RSA PRIVATE KEY-----"
    }.getBytes("UTF-8")
    val certEncoded = Base64.getUrlEncoder.encodeToString(cert.getEncoded)

    val jsonResponse =
      s"""{
     |"status": "ok",
     |"version": {
     |  "comment": "\u0421\u0435\u0440\u0442\u0438\u0444\u0438\u043a\u0430\u0442\u044b \u0434\u043b\u044f \u043f\u043e\u0434\u043f\u0438\u0441\u0438 \u0438 \u0432\u0435\u0440\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u0438 \u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438 \u0447\u0435\u0440\u0435\u0437 \u0433\u043e\u0441\u0443\u0441\u043b\u0443\u0433\u0438",
     |  "created_at": 1599216818.283,
     |  "created_by": 1120000000254770,
     |  "creator_login": "fripe",
     |  "secret_name": "autoru-gosuslugi-auth-certificate",
     |  "secret_uuid": "sec-01ehcc0633yvwmqa1fz1fvmd66",
     |  "value": [
     |{
     |  "encoding": "base64",
     |  "key": "certificate-test",
     |  "value": "${(certEncoded)}"
     |},
     |{
     |  "encoding": "base64",
     |  "key": "privatekey-test",
     |  "value": "${(Base64.getUrlEncoder.encodeToString(keyEncoded))}"
     |}
     |  ],
     |  "version": "ver-01ehcc063bhpqzw26f03gnfkh8"
     |}
   | }""".stripMargin
    val yavClient = mock[YavClient]
    /*
    Генерация тестовых сертификатов выключена, мы не умеем генерировать сертификаты с гост шифрованием
     */
    when(yavClient.getSecrets(?)(?)).thenReturn(Future.successful(Some(Json.parse(jsonResponse))))
    val mockedGosuslugiSigner = mock[GosuslugiSigner]
    when(mockedGosuslugiSigner.signData(?)).thenReturn("signed")
    code(new GosuslugiClient(cfg, http, mockedGosuslugiSigner))
  }

  def withDefaults[T](code: (GosuslugiConfig, GosuslugiClient) => T): T = withConfig { cfg =>
    withClient(cfg) { client =>
      code(cfg, client)
    }
  }
}
