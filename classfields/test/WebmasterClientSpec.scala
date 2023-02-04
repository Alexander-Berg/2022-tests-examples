package ru.vertistraf.cost_plus.common.webmaster

import common.zio.sttp.Sttp
import common.zio.sttp.endpoint.Endpoint
import ru.vertistraf.cost_plus.webmaster.client.WebmasterClient
import ru.vertistraf.cost_plus.webmaster.client.live.LiveWebmasterClient
import ru.vertistraf.cost_plus.webmaster.client.model.{AddFeedStatus, AddedFeed, DeletedFeed, FeedType, WebmasterFeed}
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.StatusCode
import zio.ZLayer
import zio.test._
import zio.test.Assertion._

object WebmasterClientSpec extends DefaultRunnableSpec {

  private val Feed1 = WebmasterFeed(
    url = "https://auto.ru/cost-plus/marks_0.yml",
    `type` = FeedType.Cars,
    regionIds = Seq(225)
  )

  private val Feed2 = WebmasterFeed(
    url = "https://auto.ru/cost-plus/marks_1.yml",
    `type` = FeedType.Cars,
    regionIds = Seq(225)
  )

  private val StubHostId = "hostId"

  private val EscapeHostId: Long = 1L
  private val ReturnMalformedTotal: Long = 2L
  private val ReturnMalformedError: Long = 3L
  private val ReturnMalformedResponse: Long = 4L
  private val ReturnErrorResponse: Long = 5L
  private val ReturnListFeeds: Long = 6L
  private val ReturnAddBatch: Long = 7L
  private val ReturnRemove: Long = 8L

  private val ForEscapeHost = s"v4/user/$EscapeHostId/hosts/https%3Aauto.ru%3A443/feeds/list".split("/").toSeq
  private val ForReturnMalformedTotal = s"v4/user/$ReturnMalformedTotal/hosts/$StubHostId/feeds/list".split("/").toSeq
  private val ForReturnMalformedError = s"v4/user/$ReturnMalformedError/hosts/$StubHostId/feeds/list".split("/").toSeq

  private val ForReturnMalformedResponse =
    s"v4/user/$ReturnMalformedResponse/hosts/$StubHostId/feeds/list".split("/").toSeq
  private val ForReturnErrorResponse = s"v4/user/$ReturnErrorResponse/hosts/$StubHostId/feeds/list".split("/").toSeq
  private val ForReturnListFeed = s"v4/user/$ReturnListFeeds/hosts/$StubHostId/feeds/list".split("/").toSeq
  private val ForReturnAddBatch = s"v4/user/$ReturnAddBatch/hosts/$StubHostId/feeds/batch/add".split("/").toSeq
  private val ForReturnRemove = s"v4/user/$ReturnRemove/hosts/$StubHostId/feeds/batch/remove".split("/").toSeq

  private val stub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case r if r.uri.path.equals(ForEscapeHost) =>
      Response.ok("""{"feeds":[]}""")
    case r if r.uri.path.equals(ForReturnMalformedTotal) =>
      Response("""{"feeds": []""", StatusCode.InternalServerError)
    case r if r.uri.path.equals(ForReturnMalformedError) =>
      Response("""{"code": "BAD"}""", StatusCode.BadRequest)
    case r if r.uri.path.equals(ForReturnMalformedResponse) =>
      Response.ok("""{"feeds_list": []}""")
    case r if r.uri.path.equals(ForReturnErrorResponse) =>
      Response(
        "{\"error_message\":\"Used OAuth token is invalid or expired\",\"error_code\":\"INVALID_OAUTH_TOKEN\"}",
        StatusCode.Forbidden
      )
    case r if r.uri.path.equals(ForReturnListFeed) =>
      Response.ok(
        """{"feeds":[{"url":"https://auto.ru/cost-plus/marks_0.yml","regionIds":[225],"type":"CARS"},{"url":"https://auto.ru/cost-plus/marks_1.yml","regionIds":[225],"type":"CARS"}]}"""
      )

    case r if r.uri.path.equals(ForReturnAddBatch) =>
      Response.ok(
        """{"feeds":[{"url":"https://auto.ru/cost-plus/marks_0.yml","status":"OK"},{"url":"https://auto.ru/cost-plus/marks_1.yml","status":"FEED_ALREADY_ADDED"}]}"""
      )

    case r if r.uri.path.equals(ForReturnRemove) =>
      Response.ok(
        """{"feeds":[{"url":"https://auto.ru/cost-plus/marks_0.yml","status":"OK"},{"url":"https://auto.ru/cost-plus/marks_1.yml","status":"OK"}]}"""
      )

  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("WebmasterClient")(
      testM("should correctly escape hostId") {
        for {
          res <- WebmasterClient.listFeeds(EscapeHostId, "https:auto.ru:443")
        } yield assertTrue(res.isEmpty)
      },
      testM("should correctly return malformed total") {
        WebmasterClient
          .listFeeds(ReturnMalformedTotal, StubHostId)
          .run
          .map { r =>
            assert(r)(fails(isSubtype[WebmasterClient.Error.MalformedResponse](anything)))
          }
      },
      testM("should correctly return malformed error") {
        WebmasterClient
          .listFeeds(ReturnMalformedError, StubHostId)
          .run
          .map { r =>
            assert(r)(fails(isSubtype[WebmasterClient.Error.MalformedResponse](anything)))
          }
      },
      testM("should correctly return malformed response") {
        WebmasterClient
          .listFeeds(ReturnMalformedResponse, StubHostId)
          .run
          .map { r =>
            assert(r)(fails(isSubtype[WebmasterClient.Error.MalformedResponse](anything)))
          }
      },
      testM("should correctly return error response") {
        WebmasterClient
          .listFeeds(ReturnErrorResponse, StubHostId)
          .run
          .map { r =>
            assert(r)(
              fails(
                equalTo(
                  WebmasterClient.Error.KnownError(
                    code = "INVALID_OAUTH_TOKEN",
                    message = "Used OAuth token is invalid or expired"
                  )
                )
              )
            )
          }
      },
      testM("should correctly return list of feeds") {
        WebmasterClient
          .listFeeds(ReturnListFeeds, StubHostId)
          .map(res => assert(res)(hasSameElements(Seq(Feed1, Feed2))))
      },
      testM("should correctly return added feeds") {
        WebmasterClient
          .addFeeds(ReturnAddBatch, StubHostId, Seq(Feed1, Feed2))
          .map { res =>
            assert(res)(
              hasSameElements(
                Seq(AddedFeed(Feed1.url, AddFeedStatus.Ok), AddedFeed(Feed2.url, AddFeedStatus.FeedAlreadyAdded))
              )
            )
          }
      },
      testM("should correctly return remove") {
        WebmasterClient
          .removeFeeds(ReturnRemove, StubHostId, Set(Feed1.url, Feed2.url))
          .map(res =>
            assert(res)(
              hasSameElements(
                Seq(
                  DeletedFeed(Feed1.url, "OK"),
                  DeletedFeed(Feed2.url, "OK")
                )
              )
            )
          )
      }
    ).provideLayer {
      Sttp.fromStub(stub) ++ ZLayer.succeed(
        LiveWebmasterClient.Config(
          endpoint = Endpoint(host = "wm.host", port = 80),
          token = "token"
        )
      ) >>> WebmasterClient.live
    }
}
