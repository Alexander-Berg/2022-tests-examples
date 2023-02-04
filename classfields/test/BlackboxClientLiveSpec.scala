package common.clients.blackbox.test

import common.clients.blackbox.model.{
  BlackboxException,
  Email,
  Phone,
  PhoneAttribute,
  UserInfo,
  UserInfoError,
  UserNotFound
}
import common.clients.blackbox.{BlackboxClient, BlackboxClientLive}
import common.tvm.model.UserTicket.TicketBody
import common.zio.sttp.endpoint.Endpoint
import common.zio.sttp.Sttp
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.test.Assertion._
import zio.test._

import scala.io.Source

object BlackboxClientLiveSpec extends DefaultRunnableSpec {

  private val validStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case r if r.uri.paramsMap.get("user_ticket").contains(errorTicket.body) =>
      Response.ok(Source.fromResource("erroranswer.json")(scala.io.Codec.UTF8).getLines().mkString)
    case _ =>
      Response.ok(Source.fromResource("multianswer.json")(scala.io.Codec.UTF8).getLines().mkString)
  }

  // ./ya tools tvmknife unittest user --default 1
  private val ticket =
    TicketBody(
      "3:user:CA0Q__________9_Gg4KAggBEAEg0oXYzAQoAQ:FUF1bTMjaFuxEyM1-TsldpgNPJ8mXrLR0fgdo-PQ_faY_gvS1wqbzRek0mLUWmhoShgwrUinCIHCfEIPhyTZcBHFhZ4FCm1vkSHPfARZKnf0Ok6aIw2FFWPvmoulU25nwDgL6UWKz-30IgAyTRReotwlOd8jZO_LKWCZT86c8nox"
    )

  private val errorTicket = TicketBody("error")

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("BlackboxClientLive")(
      testM("parse response") {
        for {
          response <- BlackboxClient.userInfoByTicket(ticket, 1, phoneAttributes = Seq(PhoneAttribute.E164))
        } yield assert(response)(
          equalTo(
            List(
              UserInfoError(
                11806301,
                BlackboxException(10, "DB_EXCEPTION"),
                "Fatal BlackBox error: dbpool exception in sezam dbfields fetch"
              ),
              UserNotFound(400001328821L),
              UserInfo(
                3000062912L,
                Some("Козьма П."),
                Some("4000217463"),
                Some(85),
                List(Email(address = "test@yandex.ru")),
                List(Phone(Map(PhoneAttribute.E164 -> "+79999999999"))),
                publicId = Some("mcat26m4cb7z951vv46zcbzgqt")
              )
            )
          )
        )
      },
      testM("parse error") {
        BlackboxClient.userInfoByTicket(errorTicket, 2).run.map { response =>
          assert(response)(fails(hasMessage(containsString("ACCESS_DENIED"))))
        }
      }
    ).provideCustomLayer((Endpoint.testEndpointLayer ++ Sttp.fromStub(validStub)) >>> BlackboxClientLive.Live)
  }
}
