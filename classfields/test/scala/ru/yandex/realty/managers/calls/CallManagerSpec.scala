package ru.yandex.realty.managers.calls

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.response.Response.OfferResponse
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.call.{CallInfo, CallListResponse, CallResponse, CallSearchRequest}
import ru.yandex.realty.clients.billing.BillingClient
import ru.yandex.realty.clients.capa.CapaClient
import ru.yandex.realty.clients.statistics.RawStatisticsClient
import ru.yandex.realty.clients.vos.RequestModel.{UnifiedLocation, VosOffer, VosOfferList}
import ru.yandex.realty.clients.vos.VosClient
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.managers.lk.stats.{CallManager, CallStatisticsAdapter}
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.duration.TimeRange
import ru.yandex.realty.model.request.Page
import ru.yandex.realty.proto.offer.{OfferType => ProtoOfferType}
import ru.yandex.realty.util.protobuf.TimeProtoFormats._
import ru.yandex.vertis.paging.{Slice, Slicing}
import ru.yandex.vertis.paging.Slice.{Page => ProtoPage}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class CallManagerSpec extends AsyncSpecBase {
  val vosClient: VosClient = mock[VosClient]
  val rawStatisticsClient: RawStatisticsClient = mock[RawStatisticsClient]
  val capaClient: CapaClient = mock[CapaClient]
  val billingClient: BillingClient = mock[BillingClient]

  val adapter = new CallStatisticsAdapter(rawStatisticsClient)
  val manager = new CallManager(vosClient, rawStatisticsClient, adapter, capaClient, billingClient)

  "Call manager" should {
    "make correct address" in {
      (capaClient
        .getPartners(_: String)(_: Traced))
        .expects("91387577", *)
        .returning(Future.successful(Nil))

      (rawStatisticsClient
        .getFlatCallList(_: CallSearchRequest, _: Option[Page])(_: Traced))
        .expects(*, *, *)
        .returning(
          Future.successful(
            CallListResponse
              .newBuilder()
              .addCalls(
                CallResponse.newBuilder
                  .setInfo(
                    CallInfo.newBuilder
                      .setTimestamp(DateTimeFormat.write(DateTime.parse("2018-03-05T14:36:48.600Z")))
                      .setCallType(ru.yandex.realty.call.CallType.TARGET)
                      .setDuration(DurationProtoFormat.write(108.seconds))
                      .setSourcePhone("+79998204020")
                      .setDestinationPhone("+74952326969")
                  )
                  .setOffer(
                    OfferResponse.newBuilder
                      .setOfferId("4696637666977379629")
                      .setOfferType(ProtoOfferType.SELL)
                  )
              )
              .setSlicing(
                Slicing
                  .newBuilder()
                  .setTotal(412)
                  .setSlice(Slice.newBuilder().setPage(ProtoPage.newBuilder().setNum(0).setSize(10)))
              )
              .build()
          )
        )

      (vosClient
        .getOffersById(_: String, _: Seq[String])(_: Traced))
        .expects(*, *, *)
        .returning(
          Future.successful(
            VosOfferList(
              List(
                VosOffer(
                  "4696637666977379629",
                  OfferType.SELL,
                  CategoryType.LOT,
                  "",
                  Some(
                    UnifiedLocation(
                      Some("Москва"),
                      Some("поселение Вороновское"),
                      Some("посёлок дома отдыха Вороново"),
                      None,
                      None,
                      None
                    )
                  )
                )
              )
            )
          )
        )

      val calls = manager.getUserCalls(
        "91387577",
        None,
        Some(Page(0, 10)),
        TimeRange(
          Some(DateTime.parse("2016-05-01T13:18:18.478+03:00")),
          Some(DateTime.parse("2020-05-15T13:18:18.478+03:00"))
        ),
        List(),
        None,
        None,
        List(),
        Set.empty
      )(Traced.empty)

      val awaited = Await.result(calls, Duration.Inf)

      require(awaited.getCalls(0).getOffer.getLocation.getAddress == "посёлок дома отдыха Вороново")
      val expectedResult =
        """calls {
          |  info {
          |    timestamp {
          |      seconds: 1520260608
          |      nanos: 600000000
          |    }
          |    call_type: TARGET
          |    duration {
          |      length: 108
          |      time_unit: SECONDS
          |    }
          |    source_phone: "+79998204020"
          |    destination_phone: "+74952326969"
          |  }
          |  offer {
          |    offer_id: "4696637666977379629"
          |    offer_type: SELL
          |    offer_category: LOT
          |    location {
          |      address: "\320\277\320\276\321\201\321\221\320\273\320\276\320\272 \320\264\320\276\320\274\320\260 \320\276\321\202\320\264\321\213\321\205\320\260 \320\222\320\276\321\200\320\276\320\275\320\276\320\262\320\276"
          |    }
          |  }
          |}
          |slicing {
          |  slice {
          |    page {
          |      size: 10
          |    }
          |  }
          |  total: 412
          |}
          |""".stripMargin

      awaited.toString shouldEqual expectedResult
    }
  }
}
