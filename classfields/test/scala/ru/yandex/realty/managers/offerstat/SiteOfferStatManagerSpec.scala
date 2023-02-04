package ru.yandex.realty.managers.offerstat

import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.adsource.{AdSource, AdSourceService, AdSourceType}
import ru.yandex.realty.api.ProtoResponse.SiteOfferStatResponse
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.errors.CommonError
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.offerstat.SiteOfferStatFetchTypes
import ru.yandex.realty.offerstat.SiteOfferStatFetchTypes.SiteOfferStatFetchType
import ru.yandex.realty.proto.RoomsType
import ru.yandex.realty.proto.search.{ApartmentFilter, ApartmentRoomsEntry, SiteOfferStat}
import ru.yandex.realty.search.offerstat.SiteOfferStatQuery
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class SiteOfferStatManagerSpec extends AsyncSpecBase with RequestAware {

  private val searcherClient = mock[SearcherClient]
  private val adSourceService = mock[AdSourceService]
  private val manager = new SiteOfferStatManager(searcherClient, adSourceService)

  private val fetchType = SiteOfferStatFetchTypes.FetchTypeEverything

  private val genericFailure =
    Future.failed(CommonError("UNKNOWN_ERROR", "Internal Server Error", StatusCodes.InternalServerError))

  private val basicStats =
    Future.successful(
      SiteOfferStatResponse
        .newBuilder()
        .setResponse(
          SiteOfferStat
            .newBuilder()
            .setAllFilters(
              ApartmentFilter
                .newBuilder()
                .addAllFloors((1 to 5).map(Int.box).toList.asJava)
                .addAllFirstFloor(Iterable(false, true).map(Boolean.box).asJava)
                .addAllLastFloor(Iterable(false, true).map(Boolean.box).asJava)
                .build()
            )
            .setDynamicFilters(
              ApartmentFilter
                .newBuilder()
                .addAllFloors(Iterable(2, 5).map(Int.box).asJava)
                .addAllFirstFloor(Iterable(false).map(Boolean.box).asJava)
                .addAllLastFloor(Iterable(false, true).map(Boolean.box).asJava)
                .build()
            )
            .addAllRooms(
              Iterable(
                ApartmentRoomsEntry
                  .newBuilder()
                  .addFloors(5)
                  .setRoomsType(RoomsType.newBuilder().setRooms(RoomsType.Rooms.newBuilder().setCount(4).build()))
                  .setApartments(2)
                  .build()
              ).asJava
            )
            .build()
        )
        .build()
    )

  "SiteOfferStatManager" should {

    "fail when searcher fails" in {
      val userRef = UserRef.app("device")
      val psq = SiteOfferStatQuery(13L, primarySale = Some(true))

      (searcherClient
        .getPrimarySaleRoomsStat(_: SiteOfferStatQuery, _: SiteOfferStatFetchType)(_: Traced))
        .expects(psq, fetchType, *)
        .returning(genericFailure)

      interceptCause[CommonError] {
        withRequestContext(userRef) { implicit r =>
          manager
            .primarySaleRoomsStat(psq, fetchType)
            .futureValue
        }
      }
    }

    "return response from searcher" in {
      val userRef = UserRef.web("2386512367")
      val psq = SiteOfferStatQuery(42L, primarySale = Some(true))

      (searcherClient
        .getPrimarySaleRoomsStat(_: SiteOfferStatQuery, _: SiteOfferStatFetchType)(_: Traced))
        .expects(psq, fetchType, *)
        .returning(basicStats)

      withRequestContext(userRef) { implicit r =>
        val result = manager.primarySaleRoomsStat(psq, fetchType).futureValue
        result.hasError should be(false)
        result.hasResponse should be(true)
        result.getResponse should be(basicStats.futureValue.getResponse)
      }
    }

    "consider adSource parameter in request" in {
      val userRef = UserRef.web("2386512367")
      val paidOnly = Some("ad_source")
      val searcherRequest = SiteOfferStatQuery(42L, primarySale = Some(true))
      val adSource = AdSource(AdSourceType.YANDEX_DIRECT, System.currentTimeMillis())

      (searcherClient
        .getSiteOfferStat(
          _: SiteOfferStatQuery,
          _: SiteOfferStatFetchType,
          _: Option[String],
          _: Option[String],
          _: Option[Int]
        )(_: Traced))
        .expects(searcherRequest, fetchType, None, paidOnly, None, *)
        .returning(basicStats)
        .once()

      (adSourceService
        .getPaidOnlyParamOptExceptSpecialPartner(_: Option[AdSource], _: Option[Long]))
        .expects(Some(adSource), None)
        .once()
        .returning(paidOnly)

      withRequestContext(userRef) { implicit r =>
        val statQuery = SiteOfferStatQuery(42L, primarySale = Some(true))
        val result = manager
          .siteOfferStat(
            statQuery,
            fetchType,
            None,
            Some(adSource)
          )
          .futureValue
        result.hasError should be(false)
        result.hasResponse should be(true)
        result.getResponse should be(basicStats.futureValue.getResponse)
      }
    }

  }

}
