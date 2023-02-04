package ru.yandex.vertis.passport.api.v2.service.tokens

import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import com.google.protobuf.util.Timestamps
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.joda.time.DateTimeZone
import org.mockito.Mockito.verify
import org.scalacheck.Gen
import org.scalatest.WordSpec
import play.api.libs.json.{JsValue, Json}
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel.{ApiTokenHistoriesResponse, ApiTokenHistoryEntry, ApiTokenHistoryInfo, ApiTokenHistoryListRequest, ApiTokenPayload}
import ru.yandex.vertis.generators.DateTimeGenerators
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.Future
import scala.util.Random
import ru.yandex.vertis.passport.api.NoTvmAuthorization

class ApiTokensHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with PlayJsonSupport
  with V2Spec
  with NoTvmAuthorization {
  val base = "/api/2.x/auto/api-tokens"

  "ApiTokenHandler" should {
    "create api token" in {
      val params = ModelGenerators.ApiTokenCreateParamsGen.next
      val result = ModelGenerators.ApiTokenResultGen.next

      when(apiTokenService.createToken(?)(?)).thenReturn(Future.successful(result))

      Post(base, params) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.ApiTokenResult]
          response shouldBe result

          verify(apiTokenService).createToken(eq(params))(?)
        }
    }

    "get token" in {
      val id = ModelGenerators.ApiTokenGen.next.token
      val result = ModelGenerators.ApiTokenResultGen.next

      when(apiTokenService.getToken(?)(?)).thenReturn(Future.successful(result))

      Get(s"$base/$id") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.ApiTokenResult]
          response shouldBe result

          verify(apiTokenService).getToken(eq(id))(?)
        }
    }

    "get 404 if token not found" in {
      val id = ModelGenerators.ApiTokenGen.next.token

      when(apiTokenService.getToken(?)(?)).thenReturn(
        Future.failed(new NoSuchElementException(""))
      )

      Get(s"$base/$id") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe NotFound

          verify(apiTokenService).getToken(eq(id))(?)
        }
    }

    "list history" in {
      def nextStr = Gen.alphaStr.next.take(5)
      def nextInt = Random.nextInt(1e7.toInt)

      val token = nextStr
      val grant = nextStr
      val limitPerIp = nextInt
      val requester = nextStr
      val comment = nextStr
      val version = nextInt

      val expectedRequestBody = ApiTokenHistoryListRequest
        .newBuilder()
        .addTokens(token)
        .build()

      val payload = ApiTokenPayload
        .newBuilder()
        .addGrants(grant)
        .setRatelimitPerIp(limitPerIp)
        .build()

      val dt = DateTimeGenerators.dateTime().next
      val moment = Timestamps.fromMillis(dt.getMillis)

      val oneFoundHistoryElem = ApiTokenHistoryInfo
        .newBuilder()
        .setMoment(moment)
        .setRequester(requester)
        .setComment(comment)
        .setPayload(payload)
        .setVersion(version)
        .build()

      val entry = ApiTokenHistoryEntry
        .newBuilder()
        .setToken(token)
        .addHistory(oneFoundHistoryElem)
        .build()

      val expectedResponseBody = ApiTokenHistoriesResponse
        .newBuilder()
        .addTokenHistory(entry)
        .build()

      when(apiTokenService.listHistory(?)(?)).thenReturn(
        Future.successful(expectedResponseBody)
      )

      Post(s"$base/history/list", expectedRequestBody) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        route ~>
        check {
          status shouldBe OK
          responseAs[JsValue] shouldBe Json.parse(
            s"""
              |{
              |  "tokenHistory": [
              |    {
              |     "token": "$token",
              |      "history": [
              |        {
              |          "moment": "${dt.withZone(DateTimeZone.forOffsetHours(0))}",
              |          "requester": "$requester",
              |          "comment": "$comment",
              |          "payload": {
              |            "ratelimitPerIp": $limitPerIp,
              |            "grants": [
              |              "$grant"
              |            ]
              |          },
              |          "version": "$version"
              |        }
              |      ]
              |    }
              |  ]
              |}
              |""".stripMargin
          )

          verify(apiTokenService).listHistory(eq(expectedRequestBody))(?)

        }
    }
  }
}
