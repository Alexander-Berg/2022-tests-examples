package ru.auto.api.services.vox

import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.auth.Application
import ru.auto.api.model._
import ru.auto.api.services.vox.VoxClient.{AddUserResponse, ErrorDetails, ErrorResponse, UserDetails}
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.StringUtils.UrlInterpolation

class DefaultVoxClientSpec extends HttpClientSpec with MockedHttpClient with ScalaCheckPropertyChecks {

  "addUser" should {
    "send correct request and parse successful response" in new Wiring {
      forAll(Gen.hexStr, Gen.alphaStr, Gen.hexStr, Arbitrary.arbLong.arbitrary, Arbitrary.arbLong.arbitrary) {
        (username, userDisplayName, password, voxResultVal, voxUserId) =>

          val expectedUrl = s"/platform_api/AddUser/?" +
            url"user_name=$username&user_display_name=$userDisplayName&user_password=$password&" +
            url"account_id=$sampleAccountId&api_key=$sampleApiKey&application_name=$sampleApplicationName"

          val sampleResponse =
            s"""{
               |	"result": $voxResultVal,
               |	"user_id": $voxUserId
               |}""".stripMargin

          http.expectUrl(expectedUrl)
          http.respondWith(sampleResponse)

          val res = voxClient.addUser(username, userDisplayName, password)(req).await
          res shouldBe Right(AddUserResponse(voxResultVal, voxUserId))
      }
    }

    "correctly parse error response" in new Wiring {
      forAll(Gen.hexStr, Gen.alphaStr, Gen.hexStr, Arbitrary.arbLong.arbitrary, Gen.alphaStr) {
        (username, userDisplayName, password, errorCode, errorMessage) =>
          val expectedUrl = s"/platform_api/AddUser/?" +
            url"user_name=$username&user_display_name=$userDisplayName&user_password=$password&" +
            url"account_id=$sampleAccountId&api_key=$sampleApiKey&application_name=$sampleApplicationName"

          val sampleResponse =
            s"""{
               |  "error": {
               |    "msg": "$errorMessage",
               |    "code": $errorCode,
               |    "field_name": "user_name"
               |  },
               |  "errors": [
               |    {
               |      "msg": "User name isn't unique.",
               |      "code": 118,
               |      "field_name": "user_name"
               |    }
               |  ]
               |}""".stripMargin

          http.expectUrl(expectedUrl)
          http.respondWith(sampleResponse)

          val res = voxClient.addUser(username, userDisplayName, password)(req).await
          res shouldBe Left(ErrorResponse(ErrorDetails(errorMessage, errorCode)))
      }
    }

    "getUser" should {
      "return user if exists" in new Wiring {
        val username = "u_name"
        val expectedUrl = s"/platform_api/GetUsers/?" +
          url"user_name=$username&" +
          url"account_id=$sampleAccountId&api_key=$sampleApiKey&application_name=$sampleApplicationName"

        val sampleResponse = s"""{
                                |	"result": [
                                |		{
                                |			"user_active": true,
                                |			"balance": 1,
                                |			"user_id": 1,
                                |			"user_name": "$username",
                                |			"user_display_name": "iden-1",
                                |			"frozen": false,
                                |			"modified": "2013-09-09 11:26:31"
                                |		}
                                |	],
                                |	"count": 1,
                                |	"total_count": 1
                                |}""".stripMargin

        http.expectUrl(expectedUrl)
        http.respondWith(sampleResponse)

        val res = voxClient.getUser(username)(req).await
        res shouldBe Some(UserDetails(user_active = true, 1, 1, username, "iden-1", frozen = false))

      }

      "return None if not exists" in new Wiring {
        val username = "u_name"
        val expectedUrl = s"/platform_api/GetUsers/?" +
          url"user_name=$username&" +
          url"account_id=$sampleAccountId&api_key=$sampleApiKey&application_name=$sampleApplicationName"

        val sampleResponse = s"""{
                                |	"result": [
                                |	],
                                |	"count": 1,
                                |	"total_count": 1
                                |}""".stripMargin

        http.expectUrl(expectedUrl)
        http.respondWith(sampleResponse)

        val res = voxClient.getUser(username)(req).await
        res shouldBe None
      }
    }
  }

  trait Wiring {

    implicit protected val req: RequestImpl = {
      val r = new RequestImpl
      r.setTrace(trace)
      r.setApplication(Application.iosApp)
      r.setRequestParams(RequestParams.empty)
      r
    }

    val sampleAccountId = Arbitrary.arbLong.arbitrary.next
    val sampleApiKey = Gen.identifier.next
    val sampleApplicationName = Gen.identifier.next

    val voxClient = new DefaultVoxClient(http, sampleAccountId.toString, sampleApiKey, sampleApplicationName)

  }

}
