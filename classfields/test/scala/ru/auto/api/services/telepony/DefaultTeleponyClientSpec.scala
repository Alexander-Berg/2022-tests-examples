package ru.auto.api.services.telepony

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import org.apache.http.client.utils.URIBuilder
import org.scalatest.OptionValues
import ru.auto.api.ResponseModel.CallHistoryResponse.{CallHistoryItem, CallResult}
import ru.auto.api.ResponseModel.{CallHistoryResponse, ResponseStatus}
import ru.auto.api.exceptions.TeleponyNoAvailableRedirectPhoneNumber
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.Paging
import ru.auto.api.model.gen.BasicGenerators.readableString
import ru.auto.api.services.telepony.TeleponyClient._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.Protobuf
import ru.yandex.vertis.paging.Slice
import ru.yandex.vertis.telepony.model.proto.{CallResponse, SourceLastCallsRequest, SourceLastCallsResponse}
import ru.yandex.vertis.{paging => vertis}

import java.net.URLEncoder
import java.time.OffsetDateTime
import scala.jdk.CollectionConverters._

/**
  * @author pnaydenov
  */
class DefaultTeleponyClientSpec extends HttpClientSpec with MockedHttpClient with OptionValues {

  val teleponyClient = new DefaultTeleponyClient(http)

  import DefaultTeleponyClient._

  "TeleponyClient" should {

    def checkActualRedirectView(redirect: Redirect): Unit = {
      redirect.id shouldEqual "Ksx-NKJ6Nxw"
      redirect.source shouldEqual "+79169040152"
      redirect.target shouldEqual "+70000000000"
      redirect.objectId shouldEqual "1"
      redirect.createTime shouldEqual OffsetDateTime.parse("2017-03-09T12:46:56.900+03:00")
      redirect.deadline.get shouldEqual OffsetDateTime.parse("2017-03-09T12:56:56.900+03:00")
    }

    "get existing redirect" in {
      http.expectUrl(GET, "/api/2.x/test_domain/redirect/object-id/?pageNum=0&pageSize=10")
      http.respondWithJsonFrom("/telepony/redirect.json")

      val result = teleponyClient.redirects("test_domain", "object-id", None, None, Paging.Default).futureValue
      result should have size 1
      checkActualRedirectView(result.head)
    }

    "get existing redirect with target prefix" in {
      val targetPrefix = "+7916690"
      val encodedPrefix = URLEncoder.encode(targetPrefix, "utf-8")
      http.expectUrl(GET, s"/api/2.x/test_domain/redirect/object-id/?pageNum=0&pageSize=10&targetPhone=$encodedPrefix")
      http.respondWithStatus(StatusCodes.OK)

      teleponyClient.redirects("test_domain", "object-id", Some(targetPrefix), None, Paging.Default)
    }

    "get existing redirect with redirect prefix" in {
      val redirectPrefix = "+7916690"
      val encodedPrefix = URLEncoder.encode(redirectPrefix, "utf-8")
      http.expectUrl(GET, s"/api/2.x/test_domain/redirect/object-id/?pageNum=0&pageSize=10&proxyPhone=$encodedPrefix")
      http.respondWithStatus(StatusCodes.OK)

      teleponyClient.redirects("test_domain", "object-id", None, Some(redirectPrefix), Paging.Default)
    }

    "get non existing redirect" in {
      http.expectUrl(GET, "/api/2.x/test_domain/redirect/object-id/?pageNum=0&pageSize=10")
      http.respondWithJson("""{"total": 0,"page": {"size": 10,"number": 0},"values": []}""")

      val result = teleponyClient.redirects("test_domain", "object-id", None, None, Paging.Default).futureValue
      result should have size 0
    }

    "do get or create" in {
      http.expectUrl(POST, "/api/2.x/another_test_domain/redirect/getOrCreate/object-id/")
      http.expectJson(
        """{"target":"+70000000000","phoneType":"Mobile","geoId":1,"ttl":600,"options":{"allowRedirectUnsuccessful":false}}"""
      )
      http.respondWithJsonFrom("/telepony/get_or_else.json")

      val request =
        CreateRequest(
          "+70000000000",
          Some(PhoneTypes.Mobile),
          Some(1),
          Some(600),
          None,
          None,
          RedirectOptions(allowRedirectUnsuccessful = Some(false))
        )
      val result = teleponyClient.getOrCreate("another_test_domain", "object-id", request).futureValue
      checkActualRedirectView(result)
    }

    "do get or create without allowRedirectUnsuccessful in response" in {
      http.expectUrl(POST, "/api/2.x/another_test_domain/redirect/getOrCreate/object-id/")
      http.expectJson(
        """{"target":"+70000000000","phoneType":"Mobile","geoId":1,"ttl":600,"options":{"allowRedirectUnsuccessful":false}}"""
      )
      http.respondWithJson("""{
                             |  "source": "+79169040152",
                             |  "createTime": "2017-03-09T12:46:56.900+03:00",
                             |  "deadline": "2017-03-09T12:56:56.900+03:00",
                             |  "id": "Ksx-NKJ6Nxw",
                             |  "target": "+70000000000",
                             |  "objectId": "1",
                             |  "options": {}
                             |}""".stripMargin)

      val request =
        CreateRequest(
          "+70000000000",
          Some(PhoneTypes.Mobile),
          Some(1),
          Some(600),
          None,
          None,
          RedirectOptions(allowRedirectUnsuccessful = Some(false))
        )
      val result = teleponyClient.getOrCreate("another_test_domain", "object-id", request).futureValue
      checkActualRedirectView(result)
      result.options.value.allowRedirectUnsuccessful should be(empty)
    }

    "do get or create with voxUsername in response" in {
      http.expectUrl(POST, "/api/2.x/another_test_domain/redirect/getOrCreate/object-id/")
      http.expectJson(
        """{"target":"+70000000000","phoneType":"Mobile","geoId":1,"ttl":600,"options":{"allowRedirectUnsuccessful":false,"voxUsername":"abc"}}"""
      )
      http.respondWithJson("""{
                             |  "source": "+79169040152",
                             |  "createTime": "2017-03-09T12:46:56.900+03:00",
                             |  "deadline": "2017-03-09T12:56:56.900+03:00",
                             |  "id": "Ksx-NKJ6Nxw",
                             |  "target": "+70000000000",
                             |  "objectId": "1",
                             |  "options": {}
                             |}""".stripMargin)

      val request =
        CreateRequest(
          "+70000000000",
          Some(PhoneTypes.Mobile),
          Some(1),
          Some(600),
          None,
          None,
          RedirectOptions(allowRedirectUnsuccessful = Some(false), voxUsername = Some("abc"))
        )
      val result = teleponyClient.getOrCreate("another_test_domain", "object-id", request).futureValue
      checkActualRedirectView(result)
      result.options.value.allowRedirectUnsuccessful should be(empty)
    }

    "do get or create with tag" in {
      http.expectUrl(POST, "/api/2.x/test_domain/redirect/getOrCreate/object-id/")
      http.expectJson(
        """{"target":"+70000000000","phoneType":"Mobile","geoId":1,"ttl":600,"tag":"ururu","options":{"allowRedirectUnsuccessful":false}}"""
      )
      http.respondWithJsonFrom("/telepony/get_or_else.json")

      val request =
        CreateRequest(
          "+70000000000",
          Some(PhoneTypes.Mobile),
          Some(1),
          Some(600),
          None,
          Some("ururu"),
          RedirectOptions(allowRedirectUnsuccessful = Some(false))
        )
      val result = teleponyClient.getOrCreate("test_domain", "object-id", request).futureValue
      checkActualRedirectView(result)
    }

    "do get or create with response code 449" in {
      http.expectUrl(POST, "/api/2.x/another_test_domain/redirect/getOrCreate/object-id/")
      http.expectJson(
        """{"target":"+70000000000","phoneType":"Mobile","geoId":1,"ttl":600,"options":{"allowRedirectUnsuccessful":false}}"""
      )
      http.respondWithStatus(StatusCodes.RetryWith)

      val request =
        CreateRequest(
          "+70000000000",
          Some(PhoneTypes.Mobile),
          Some(1),
          Some(600),
          None,
          None,
          RedirectOptions(allowRedirectUnsuccessful = Some(false))
        )
      val result = intercept[TeleponyNoAvailableRedirectPhoneNumber] {
        teleponyClient.getOrCreate("another_test_domain", "object-id", request).await
      }
      assert(result.status == 449)
    }

    "do get or create with allow redirect unsuccessful" in {
      http.expectUrl(POST, "/api/2.x/another_test_domain/redirect/getOrCreate/object-id/")
      http.expectJson(
        """{"target":"+70000000000","phoneType":"Mobile","geoId":1,"ttl":600,"options":{"allowRedirectUnsuccessful":true}}"""
      )
      http.respondWithJsonFrom("/telepony/get_or_else.json")

      val request =
        CreateRequest(
          "+70000000000",
          Some(PhoneTypes.Mobile),
          Some(1),
          Some(600),
          None,
          None,
          RedirectOptions(allowRedirectUnsuccessful = Some(true))
        )
      val result = teleponyClient.getOrCreate("another_test_domain", "object-id", request).futureValue
      checkActualRedirectView(result)
    }

    "get available phones" in {
      http.expectUrl(
        "/api/2.x/test_domain/redirect/available?" +
          "phone=%2B79213027807&operator-availability=true"
      )
      http.respondWithJson("{\"count\": 5}")

      val result = teleponyClient
        .getAvailable("test_domain", "79213027807", checkOperatorAvailability = true)
        .futureValue
      result shouldBe 5
    }

    "get calls stats for the period" in {
      val from = "2017-10-10T00:00:11+03:00"
      val to = "2017-10-10T11:11:11+03:00"
      http.expectUrl(
        new URIBuilder("/api/2.x/test_domain/call/stats?")
          .addParameter("object-id", "object-id")
          .addParameter("start-time", from)
          .addParameter("end-time", to)
          .build()
          .toString
      )
      http.respondWithJson(s"""{"count": 10, "lastCallTime": "$to"}""")
      val dtf = DefaultTeleponyClient.dtf
      val result = teleponyClient
        .getCallsStats("test_domain", "object-id", OffsetDateTime.parse(from, dtf), OffsetDateTime.parse(to, dtf))
        .futureValue
      result shouldBe CallsStats(10, Some(OffsetDateTime.parse(to, dtf)))
    }

    "get call history" in {
      val domain = "test_domain"
      val objectId = "com_15390696"

      http.expectUrl(
        new URIBuilder(s"/api/2.x/$domain/call")
          .addParameter("object-id", objectId)
          .addParameter("pageNum", "0")
          .addParameter("pageSize", "10")
          .build()
          .toString
      )
      http.respondWithJson(
        s"""{"total":1,"page":{"size":10,"number":0},"values":[{"duration":42,"source":"+79030148891","externalId":"5249450342","talkDuration":31,"createTime":"2018-08-15T12:39:44.258+03:00","id":"hcvhBPlz_80","proxy":"+79160393958","target":"+79162136559","objectId":"$objectId","callResult":"no-answer","time":"2018-08-15T12:38:01.614+03:00","redirectId":"tvJdEBqTZfk"}]}"""
      )

      val time = OffsetDateTime.parse("2018-08-15T12:38:01.614+03:00")
      val createTime = OffsetDateTime.parse("2018-08-15T12:39:44.258+03:00")

      val pagingBuilder = vertis.Paging
        .newBuilder()
        .setPage(Slice.Page.newBuilder().setNum(0).setSize(10))
        .setTotal(1)

      teleponyClient.calls(domain, PrivateUserRefGen.next, objectId, Paging.Default).futureValue shouldBe
        CallHistoryResponse
          .newBuilder()
          .setPaging(pagingBuilder)
          .setStatus(ResponseStatus.SUCCESS)
          .addItems(
            CallHistoryItem
              .newBuilder()
              .setSource("+79030148891")
              .setTalkDuration(31)
              .setCallResult(CallResult.NO_ANSWER)
              .setTime(time)
          )
          .build()
    }

    "get calls full" in {
      val domain = "test_domain"
      val objectId = "com_15390696"
      val time = OffsetDateTime.parse("2018-08-15T12:38:01.614+03:00")
      val createTime = OffsetDateTime.parse("2018-08-15T12:39:44.258+03:00")
      val filters = CallsFilter(time)

      http.expectUrl(
        new URIBuilder(s"/api/2.x/$domain/call/full")
          .addParameter("object-id", objectId)
          .addParameter("start-time", time.format(dtf))
          .build()
          .toString
      )
      http.respondWithJson(
        s"""[{"duration":42,"source":"+79030148891","externalId":"5249450342","talkDuration":31,"createTime":"2018-08-15T12:39:44.258+03:00","id":"hcvhBPlz_80","proxy":"+79160393958","target":"+79162136559","objectId":"$objectId","callResult":"no-answer","time":"2018-08-15T12:38:01.614+03:00","redirectId":"tvJdEBqTZfk"}]"""
      )

      teleponyClient.getCallsFull(domain, PrivateUserRefGen.next, objectId, filters).futureValue shouldBe List(
        Call(
          id = "hcvhBPlz_80",
          objectId,
          createTime,
          time,
          callResult = CallResults.NoAnswer,
          duration = 42,
          talkDuration = 31,
          recordId = None,
          redirectId = Some("tvJdEBqTZfk"),
          tag = None,
          source = "+79030148891",
          target = "+79162136559",
          proxy = "+79160393958",
          externalId = "5249450342"
        )
      )
    }

    "get source last calls for more than 100 object ids" in {
      val phone = PhoneGen.next
      val domain = readableString.next
      val objectIds = (1 to 110).map(_.toString)
      http.expectUrl(POST, s"/api/2.x/$domain/call/source-last-calls")
      http.expectJson(
        Protobuf.toJson(
          SourceLastCallsRequest
            .newBuilder()
            .setSourcePhone(phone)
            .addAllObjectId((1 to 100).map(_.toString).asJava)
            .build()
        )
      )
      http.respondWithJson(
        StatusCodes.OK,
        Protobuf.toJsonArray((1 to 100).map { i =>
          SourceLastCallsResponse.Call.newBuilder().setObjectId(i.toString).build()
        }.toList)
      )
      http.nextRequest()
      http.expectJson(
        Protobuf.toJson(
          SourceLastCallsRequest
            .newBuilder()
            .setSourcePhone(phone)
            .addAllObjectId((101 to 110).map(_.toString).asJava)
            .build()
        )
      )
      http.respondWithJson(
        StatusCodes.OK,
        Protobuf.toJsonArray((101 to 110).map { i =>
          SourceLastCallsResponse.Call.newBuilder().setObjectId(i.toString).build()
        }.toList)
      )
      val result = teleponyClient.getSourceLastCalls(phone, domain, objectIds).futureValue.toList
      http.verifyRequestDone()
      result.length shouldBe 110
      (result.map(_.getObjectId) should contain).theSameElementsInOrderAs((1 to 110).map(_.toString))
    }

    "get call history without source" in {
      val domain = "test_domain"
      val objectId = "com_15390696"

      http.expectUrl(
        new URIBuilder(s"/api/2.x/$domain/call")
          .addParameter("object-id", objectId)
          .addParameter("pageNum", "0")
          .addParameter("pageSize", "10")
          .build()
          .toString
      )
      http.respondWithJson(
        s"""{"total":1,"page":{"size":10,"number":0},"values":[{"duration":42,"externalId":"5249450342","talkDuration":31,"createTime":"2018-08-15T12:39:44.258+03:00","id":"hcvhBPlz_80","proxy":"+79160393958","target":"+79162136559","objectId":"$objectId","callResult":"no-answer","time":"2018-08-15T12:38:01.614+03:00","redirectId":"tvJdEBqTZfk"}]}"""
      )

      val time = OffsetDateTime.parse("2018-08-15T12:38:01.614+03:00")
      val createTime = OffsetDateTime.parse("2018-08-15T12:39:44.258+03:00")

      val pagingBuilder = vertis.Paging
        .newBuilder()
        .setPage(Slice.Page.newBuilder().setNum(0).setSize(10))
        .setTotal(1)

      teleponyClient.calls(domain, PrivateUserRefGen.next, objectId, Paging.Default).futureValue shouldBe
        CallHistoryResponse
          .newBuilder()
          .setPaging(pagingBuilder)
          .setStatus(ResponseStatus.SUCCESS)
          .addItems(
            CallHistoryItem
              .newBuilder()
              .setTalkDuration(31)
              .setCallResult(CallResult.NO_ANSWER)
              .setTime(time)
          )
          .build()
    }
  }

  "get call history when callType = REDIRECT_CALL" in {
    val domain = "test_domain"
    val objectId = "com_15390696"

    http.expectUrl(POST, s"/api/2.x/$domain/call/history")
    val expectValue = Protobuf.fromJson[CallResponse](s"""{
         |  "calls": [
         |    {
         |      "callId": "callId_1",
         |      "objectId": "objectId_1",
         |      "tag": "some_tag_1",
         |      "payload": {},
         |      "hasRecord": true,
         |      "domain": "autoru_def",
         |      "timestamp": "2021-06-14T09:41:55.452Z",
         |      "time": "2018-08-15T12:38:01.614+03:00",
         |      "durationSeconds": 0,
         |      "talkDurationSeconds": 31,
         |      "sourcePhone": "+798100011223344",
         |      "targetPhone": "+79810001122",
         |      "callType": "REDIRECT_CALL",
         |      "redirectCallInfo": {
         |        "redirectId": "BJdmSCl126c",
         |        "proxyNumber": "+79810001122",
         |        "callResult": "SUCCESS",
         |        "proxyGeoId": 0,
         |        "proxyOperator": "MTS",
         |        "fallbackCall": {
         |          "targetPhone": "+79810001122",
         |          "callResult": "UNKNOWN_RESULT",
         |          "time": "2021-06-14T09:41:55.452Z"
         |        }
         |      }
         |    }
         |  ]
         |}""".stripMargin)
    http.respondWithProto[CallResponse](StatusCodes.OK, expectValue)

    val actualValue =
      teleponyClient.callHistory(domain, PrivateUserRefGen.next, objectId, Paging.Default).futureValue

    actualValue shouldBe expectValue

  }

  "get call history when callType = APP_CALL" in {
    val domain = "test_domain"
    val objectId = "com_15390696"

    http.expectUrl(POST, s"/api/2.x/$domain/call/history")
    val expectValue = Protobuf.fromJson[CallResponse](
      s"""{
         |  "calls": [
         |    {
         |      "callId": "callId_1",
         |      "objectId": "objectId_1",
         |      "tag": "some_tag_1",
         |      "payload": {},
         |      "hasRecord": true,
         |      "domain": "autoru_def",
         |      "timestamp": "2021-06-14T09:41:55.452Z",
         |      "time": "2018-08-15T12:38:01.614+03:00",
         |      "durationSeconds": 0,
         |      "talkDurationSeconds": 31,
         |      "sourcePhone": "+798100011223344",
         |      "targetPhone": "+79810001122",
         |      "callType": "APP_CALL",
         |      "appCallInfo": {
         |        "sourceUsername": "string",
         |        "targetUsername": "string",
         |        "appCallResult": "SUCCESS",
         |        "phoneCall": {
         |          "phoneCallResult": "UNKNOWN_RESULT"
         |        },
         |        "redirectId": "BJdmSCl126c"
         |      }
         |    }
         |  ]
         |}""".stripMargin
    )

    http.respondWithProto(StatusCodes.OK, expectValue)

    val actualValue =
      teleponyClient.callHistory(domain, PrivateUserRefGen.next, objectId, Paging.Default).futureValue

    actualValue shouldBe expectValue
  }

  "get call history when callType = APP_CALL without talkDurationSeconds" in {
    val domain = "test_domain"
    val objectId = "com_15390696"

    http.expectUrl(POST, s"/api/2.x/$domain/call/history")
    val expectValue = Protobuf.fromJson[CallResponse](
      s"""{
         |  "calls": [
         |    {
         |      "callId": "callId_1",
         |      "objectId": "objectId_1",
         |      "tag": "some_tag_1",
         |      "payload": {},
         |      "hasRecord": true,
         |      "domain": "autoru_def",
         |      "timestamp": "2021-06-14T09:41:55.452Z",
         |      "time": "2018-08-15T12:38:01.614+03:00",
         |      "durationSeconds": 0,
         |      "sourcePhone": "+798100011223344",
         |      "targetPhone": "+79810001122",
         |      "callType": "APP_CALL",
         |      "appCallInfo": {
         |        "sourceUsername": "string",
         |        "targetUsername": "string",
         |        "appCallResult": "SUCCESS",
         |        "phoneCall": {
         |          "phoneCallResult": "UNKNOWN_RESULT"
         |        },
         |        "redirectId": "BJdmSCl126c"
         |      }
         |    }
         |  ]
         |}""".stripMargin
    )
    http.respondWithProto(StatusCodes.OK, expectValue)

    val actualValue =
      teleponyClient.callHistory(domain, PrivateUserRefGen.next, objectId, Paging.Default).futureValue

    actualValue shouldBe expectValue
  }
}
