package common.clients.personality.test

import common.clients.personality.{PersonalityClient, PersonalityClientLive}
import common.clients.personality.model.{Address, AddressType, PersonalityError}
import common.tvm.model.UserTicket.TicketBody
import common.zio.sttp.endpoint.Endpoint
import common.zio.sttp.Sttp
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.Response
import sttp.model.{Header, StatusCode}
import zio.test._
import zio.test.Assertion._

import scala.io.Source

object PersonalityClientLiveSpec extends DefaultRunnableSpec {
  private val userId = 123

  // ./ya tools tvmknife unittest user --default 123
  private val ticket =
    TicketBody(
      "3:user:CA0Q__________9_Gg4KAgh7EHsg0oXYzAQoAQ:QYCTh-Sjfbv8ERcgn86i8Zh8zxD6mg93qsxUmM0G0VeoElhsGW40ktnbKWMWOaQDxTDF8fwsZVyDIQCP5dfV1TSl4AsUdRoGKpF5pLJ7SDUWkq9l4NQMhkgZ8Ki8kfuDAyxrlgmUOFvKmUUjQRrGNG-rFdmq7-gs3SJ-2lFQskE"
    )

  private def parseResource(path: String): String = Source.fromResource(path)(scala.io.Codec.UTF8).getLines().mkString

  private val sttpStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case req
        if req.headers.contains(Header(PersonalityClientLive.userTicketHeader, ticket.body)) &&
          req.uri.path.mkString("/") == "v2/personality/profile/addresses" =>
      Response.ok(parseResource("get_addresses_response.json"))
    case req
        if req.headers.contains(Header(PersonalityClientLive.userTicketHeader, ticket.body)) &&
          req.uri.path.mkString("/") == "v2/personality/profile/addresses/home" =>
      Response.ok(parseResource("get_home_address_response.json"))
    case _ =>
      Response("Something went wrong", StatusCode.InternalServerError)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PersonalityClientLive")(
      testM("getAddresses returns addresses for existing user") {
        for {
          response <- PersonalityClient.getAddresses(userId, ticket)
        } yield assert(response)(
          equalTo(
            List(
              Address(
                "work",
                "Московская улица, 20А",
                37.13683701,
                55.85741425,
                Some("Московская улица, 20А"),
                Some("Россия, Московская область, городской округ Истра, Дедовск, Московская улица, 20А")
              ),
              Address(
                "home",
                "Большая Никольская улица",
                37.61773682,
                55.75316238,
                Some("Большая Никольская улица"),
                Some("Россия, Москва, Кремль, Большая Никольская улица")
              )
            )
          )
        )
      },
      testM("getAddresses fails if something went wrong") {
        val result = PersonalityClient.getAddresses(0, TicketBody("")).run
        assertM(result)(fails(isSubtype[PersonalityError](anything)))
      },
      testM("getAddressByType returns address for existing user") {
        for {
          response <- PersonalityClient.getAddressByType(userId, ticket, AddressType.Home)
        } yield assert(response)(
          equalTo(
            Address(
              "home",
              "Большая Никольская улица",
              37.61773682,
              55.75316238,
              Some("Большая Никольская улица"),
              Some("Россия, Москва, Кремль, Большая Никольская улица")
            )
          )
        )
      },
      testM("getAddressByType fails if something went wrong") {
        val result = PersonalityClient.getAddressByType(0, TicketBody(""), AddressType.Work).run
        assertM(result)(fails(isSubtype[PersonalityError](anything)))
      }
    ).provideCustomLayerShared((Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpStub)) >>> PersonalityClientLive.Live)
  }
}
