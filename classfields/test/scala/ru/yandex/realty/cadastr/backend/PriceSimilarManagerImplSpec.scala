package ru.yandex.realty.cadastr.backend

import com.google.protobuf.Int32Value
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.ProtoResponse.PriceStatisticResponse
import ru.yandex.realty.cadastr.dao.PriceSimilarDao
import ru.yandex.realty.cadastr.dao.PriceSimilarDao.PriceSimilar
import ru.yandex.realty.cadastr.proto.api.estimates.{
  Price,
  PriceDynamics,
  PriceRange,
  PriceSimilarRequest,
  PriceStatistic
}
import ru.yandex.realty.clients.building.BuildingSearcherClient
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.clients.prediction.PriceEstimatorClient
import ru.yandex.realty.clients.searcher.gen.SearcherResponseModelGenerators
import ru.yandex.realty.geohub.api.GeohubApi
import ru.yandex.realty.geohub.api.GeohubApi.{UnifyLocationRequest, UnifyLocationResponse}
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.building.searcher._
import ru.yandex.realty.model.building.searcher.params.Slice
import ru.yandex.realty.model.message.RealtySchema.{GeoPointMessage, LocationMessage, RawLocationMessage}
import ru.yandex.realty.model.offer.BuildingType
import ru.yandex.realty.model.serialization.LocationProtoConverter
import ru.yandex.realty.prediction.{PredictedPrice, PricePredictionLandingRequest, PricePredictionResponse}
import ru.yandex.realty.proto.offer
import ru.yandex.realty.proto.unified.offer.address.LocationUnified
import ru.yandex.realty.proto.unified.offer.objectinfos.{BuildingInfo, BuildingSeries, GeneralApartmentInfo}
import ru.yandex.realty.proto.unified.offer.offercategory.ApartmentCategory
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.lang.StringUtils.StringToIntMapper
import ru.yandex.realty.util.protobuf._
import ru.yandex.realty.util.Mappings.MapAny

import java.time.ZoneOffset
import java.util
import java.util.NoSuchElementException
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class PriceSimilarManagerImplSpec
  extends AsyncSpecBase
  with RequestAware
  with PropertyChecks
  with SearcherResponseModelGenerators {

  "PriceSimilarManagerImpl" should {

    // failed scenarios

    "throw RuntimeException if geohub client fails on unification location stage" in new Wiring with Data {
      (buildingSearcherClient
        .searchStructured(_: Map[String, List[String]])(_: Traced, _: ExecutionContext))
        .expects(sampleBuildingSearchParams, *, *)
        .returning(Future.successful(sampleRenderableSearchResponse))

      (geohubClient
        .unifyLocation(_: GeohubApi.UnifyLocationRequest)(_: Traced))
        .expects(sampleUnifyLocationRequest, *)
        .returning(Future.failed(sampleException))

      val exception: RuntimeException = interceptCause[RuntimeException] {
        priceSimilarManager.getPriceSimilar(samplePriceSimilarRequest).futureValue
      }
      exception.getMessage shouldEqual s"Can't get unified location for unified address [$sampleUnifiedAddress]"
    }

    "throw NoSuchElementException if building search client does not found building" in new Wiring with Data {
      (buildingSearcherClient
        .searchStructured(_: Map[String, List[String]])(_: Traced, _: ExecutionContext))
        .expects(sampleBuildingSearchParams, *, *)
        .returning(Future.successful(sampleEmptyRenderableSearchResponse))

      (geohubClient
        .unifyLocation(_: GeohubApi.UnifyLocationRequest)(_: Traced))
        .expects(sampleUnifyLocationRequest, *)
        .returning(Future.successful(sampleUnifyLocationResponse))

      val exception: NoSuchElementException = interceptCause[NoSuchElementException] {
        priceSimilarManager.getPriceSimilar(samplePriceSimilarRequest).futureValue
      }
      exception.getMessage shouldEqual s"Building info not found by unified address [$sampleUnifiedAddress]"
    }

    "throw RuntimeException if building search client fails on searching building stage" in new Wiring with Data {
      (buildingSearcherClient
        .searchStructured(_: Map[String, List[String]])(_: Traced, _: ExecutionContext))
        .expects(sampleBuildingSearchParams, *, *)
        .returning(Future.failed(sampleException))

      (geohubClient
        .unifyLocation(_: GeohubApi.UnifyLocationRequest)(_: Traced))
        .expects(sampleUnifyLocationRequest, *)
        .returning(Future.successful(sampleUnifyLocationResponse))

      val exception: RuntimeException = interceptCause[RuntimeException] {
        priceSimilarManager.getPriceSimilar(samplePriceSimilarRequest).futureValue
      }
      exception.getMessage shouldEqual s"Can't get building info by unified address [$sampleUnifiedAddress]"
    }

    "throw RuntimeException if price estimator client fails" in new Wiring with Data {
      (geohubClient
        .unifyLocation(_: GeohubApi.UnifyLocationRequest)(_: Traced))
        .expects(sampleUnifyLocationRequest, *)
        .returning(Future.successful(sampleUnifyLocationResponse))

      (buildingSearcherClient
        .searchStructured(_: Map[String, List[String]])(_: Traced, _: ExecutionContext))
        .expects(sampleBuildingSearchParams, *, *)
        .returning(Future.successful(sampleRenderableSearchResponse))

      (priceSimilarDao.listByAddress _)
        .expects(sampleUnifiedAddress)
        .returning(Future.successful(samplePriceSimilarList))

      (priceEstimatorClient
        .getPriceLandingPrediction(_: PricePredictionLandingRequest)(_: Traced))
        .expects(samplePriceEstimatorRequest, *)
        .returning(Future.failed(sampleException))

      val exception: RuntimeException = interceptCause[RuntimeException] {
        priceSimilarManager.getPriceSimilar(samplePriceSimilarRequest).futureValue
      }
      exception.getMessage shouldEqual
        s"Can't send request [{unified_oneline: $sampleUnifiedAddress}] to price estimator"
    }

    "throw RuntimeException if price similar dao fails" in new Wiring with Data {
      (geohubClient
        .unifyLocation(_: GeohubApi.UnifyLocationRequest)(_: Traced))
        .expects(sampleUnifyLocationRequest, *)
        .returning(Future.successful(sampleUnifyLocationResponse))

      (buildingSearcherClient
        .searchStructured(_: Map[String, List[String]])(_: Traced, _: ExecutionContext))
        .expects(sampleBuildingSearchParams, *, *)
        .returning(Future.successful(sampleRenderableSearchResponse))

      (priceSimilarDao.listByAddress _)
        .expects(sampleUnifiedAddress)
        .returning(Future.failed(sampleException))

      (priceEstimatorClient
        .getPriceLandingPrediction(_: PricePredictionLandingRequest)(_: Traced))
        .expects(samplePriceEstimatorRequest, *)
        .returning(Future.successful(samplePricePredictionResponse))

      val exception: RuntimeException = interceptCause[RuntimeException] {
        priceSimilarManager.getPriceSimilar(samplePriceSimilarRequest).futureValue
      }
      exception.getMessage shouldEqual s"Can't get price dynamics by address [$sampleUnifiedAddress]"
      exception.getCause shouldEqual sampleException
    }

    // succeed scenarios

    "get price statistic successfully" in new Wiring with Data {
      (buildingSearcherClient
        .searchStructured(_: Map[String, List[String]])(_: Traced, _: ExecutionContext))
        .expects(sampleBuildingSearchParams, *, *)
        .returning(Future.successful(sampleRenderableSearchResponse))

      (geohubClient
        .unifyLocation(_: GeohubApi.UnifyLocationRequest)(_: Traced))
        .expects(sampleUnifyLocationRequest, *)
        .returning(Future.successful(sampleUnifyLocationResponse))

      (priceSimilarDao.listByAddress _)
        .expects(sampleUnifiedAddress)
        .returning(Future.successful(samplePriceSimilarList))

      (priceEstimatorClient
        .getPriceLandingPrediction(_: PricePredictionLandingRequest)(_: Traced))
        .expects(samplePriceEstimatorRequest, *)
        .returning(Future.successful(samplePricePredictionResponse))

      val result: PriceStatisticResponse = priceSimilarManager
        .getPriceSimilar(samplePriceSimilarRequest)
        .futureValue

      result shouldEqual expectedPriceStatisticResponse
    }
  }

  trait Wiring {
    val geohubClient: GeohubClient = mock[GeohubClient]
    val buildingSearcherClient: BuildingSearcherClient = mock[BuildingSearcherClient]
    val priceEstimatorClient: PriceEstimatorClient = mock[PriceEstimatorClient]
    val priceSimilarDao: PriceSimilarDao = mock[PriceSimilarDao]

    val priceSimilarManager: PriceSimilarManager =
      new PriceSimilarManager(geohubClient, buildingSearcherClient, priceEstimatorClient, priceSimilarDao)
  }

  trait Data {
    val sampleUnifiedAddress = "some unified address"
    val sampleArea = 123.456f
    val sampleRoomsOffered = 1
    val sampleFloor: Option[Int] = None

    val samplePriceSimilarRequest: PriceSimilarRequest = PriceSimilarRequest
      .newBuilder()
      .setAddress(sampleUnifiedAddress)
      .setArea(sampleArea)
      .setRoomsOffered(sampleRoomsOffered)
      .build()

    val sampleUnifyLocationRequest: UnifyLocationRequest = UnifyLocationRequest
      .newBuilder()
      .setRawLocation(RawLocationMessage.newBuilder().setAddress(sampleUnifiedAddress))
      .build()

    val sampleGeopoint: RenderableGeoPoint = RenderableGeoPoint(10015.2f, 10016.3f)
    val sampleSubjectGenerationId: Int = 10026
    val sampleLocalityName = "sampleLocalityName"

    val sampleLocationMessage: LocationMessage = LocationMessage
      .newBuilder()
      .setGeocoderPoint(
        GeoPointMessage
          .newBuilder()
          .setLatitude(sampleGeopoint.latitude)
          .setLongitude(sampleGeopoint.longitude)
          .build()
      )
      .setGeocoderAddress(sampleUnifiedAddress)
      .setSubjectFederationId(sampleSubjectGenerationId)
      .setLocalityName(sampleLocalityName)
      .setCombinedAddress(sampleUnifiedAddress)
      .build()

    val sampleUnifyLocationResponse: UnifyLocationResponse = UnifyLocationResponse
      .newBuilder()
      .setLocation(sampleLocationMessage)
      .build()

    val sampleException = new Exception("Sample runtime exception")

    val sampleBuildingSearchParams: Map[String, List[String]] = Map("address" -> List(sampleUnifiedAddress))

    val sampleRenderableBuilding: RenderableBuilding = RenderableBuilding(
      buildingId = "sampleBuildingId",
      address = sampleUnifiedAddress,
      geoPoint = sampleGeopoint,
      metros = None,
      schools = None,
      expectedMetros = None,
      buildYear = Some(2021),
      ceilingHeight = Some(123),
      porchesCount = None,
      hasGas = None,
      heatingType = None,
      flatsCount = Some(9),
      reconstructionYear = None,
      buildingSeries = None,
      buildingSeriesId = Some(12345),
      buildingType = Some(BuildingType.BLOCK),
      floors = Some(5),
      hasElevator = Some(true),
      expectDemolition = Some(false),
      hasRubbishChute = None,
      hasSecurity = None,
      isGuarded = None,
      estimatedYearsToPayoff = None,
      buildingPriceStatistics = None
    )

    val sampleRenderableSearchResponse: RenderableSearchResponse =
      RenderableSearchResponse(
        searchQuery = RenderableSearchQuery(address = Some(sampleUnifiedAddress)),
        result = RenderableSearchResult(
          pager = RenderablePager(Slice.Full, 1),
          items = Seq(sampleRenderableBuilding)
        )
      )

    val sampleEmptyRenderableSearchResponse: RenderableSearchResponse =
      RenderableSearchResponse(
        searchQuery = RenderableSearchQuery(address = Some(sampleUnifiedAddress)),
        result = RenderableSearchResult(
          pager = RenderablePager(Slice.Full, 0),
          items = Nil
        )
      )

    val maybeType: Option[offer.BuildingType] = sampleRenderableBuilding.buildingType
      .map(_.value())
      .map(ru.yandex.realty.proto.offer.BuildingType.forNumber)

    val sampleBuildingInfo: BuildingInfo = BuildingInfo
      .newBuilder()
      .setOptLong(sampleRenderableBuilding.buildingId.toLongOption, _ setBuildingId _)
      .setOptInt(sampleRenderableBuilding.buildYear, _ setBuiltYear _)
      .setOptInt(sampleRenderableBuilding.flatsCount, _ setFlatsCount _)
      .setOptInt(sampleRenderableBuilding.floors, _ setFloorsTotal _)
      .setOptBool(sampleRenderableBuilding.expectDemolition, _ setExpectDemolition _)
      .setOptBool(sampleRenderableBuilding.hasElevator, _ setHasLift _)
      .setBuildingSeries(
        BuildingSeries
          .newBuilder()
          .applyTransformIf(
            sampleRenderableBuilding.buildingSeriesId.isDefined,
            _.setId(sampleRenderableBuilding.buildingSeriesId.get)
          )
          .build()
      )
      .applyTransformIf(
        maybeType.isDefined,
        _.setBuildingType(maybeType.get)
      )
      .build()

    val sampleApartment: ApartmentCategory = ApartmentCategory
      .newBuilder()
      .setApartmentArea(samplePriceSimilarRequest.getArea)
      .setRoomsTotal(samplePriceSimilarRequest.getRoomsOffered)
      .setBuildingInfo(sampleBuildingInfo)
      .setGeneralApartmentInfo(
        GeneralApartmentInfo
          .newBuilder()
          .setOptFloat(sampleRenderableBuilding.ceilingHeight.map(_.toFloat), _ setCeilingHeight _)
          .build()
      )
      .build()

    val sampleLocationUnified: LocationUnified = LocationProtoConverter
      .fromMessage(sampleLocationMessage)
      .extractNewModelObject()

    val samplePriceEstimatorRequest: PricePredictionLandingRequest = PricePredictionLandingRequest
      .newBuilder()
      .setApartment(sampleApartment)
      .setLocation(sampleLocationUnified)
      .build()

    val samplePredictedPrice: PredictedPrice =
      PredictedPrice
        .newBuilder()
        .setQ95(95)
        .setQ05(5)
        .setQ25(25)
        .setQ75(75)
        .setValue(50)
        .build()

    val samplePricePredictionResponse: PricePredictionResponse =
      PricePredictionResponse
        .newBuilder()
        .setPredictedPrice(samplePredictedPrice)
        .build()

    val samplePriceSimilarList: Seq[PriceSimilar] = Seq(
      PriceSimilar(
        month = "2018-10",
        priceBuilding = Some(10034.4),
        priceIso = Some(10014.4),
        priceRgid = Some(10024.4)
      ),
      PriceSimilar(
        month = "2018-10",
        priceBuilding = Some(10035.5),
        priceIso = Some(10015.5),
        priceRgid = Some(10025.5)
      ),
      PriceSimilar(
        month = "2018-11",
        priceBuilding = Some(20034.4),
        priceIso = Some(20014.4),
        priceRgid = Some(20024.4)
      ),
      PriceSimilar(
        month = "2018-11",
        priceBuilding = Some(20035.5),
        priceIso = Some(20015.5),
        priceRgid = Some(20025.5)
      )
    )

    def toProtoTimestamp(date: String): com.google.protobuf.Timestamp = {
      val seconds = java.time.YearMonth
        .parse(date)
        .atEndOfMonth()
        .atStartOfDay()
        .atOffset(ZoneOffset.UTC)
        .toEpochSecond

      com.google.protobuf.Timestamp
        .newBuilder()
        .setSeconds(seconds)
        .build()
    }

    val expectedDate1: com.google.protobuf.Timestamp = toProtoTimestamp("2018-10")
    val expectedDate2: com.google.protobuf.Timestamp = toProtoTimestamp("2018-11")

    val expectedBuildingPrices: util.List[Price] = Seq(
      Price
        .newBuilder()
        .setValue(Int32Value.of(10034))
        .setDate(expectedDate1)
        .build(),
      Price
        .newBuilder()
        .setValue(Int32Value.of(10036))
        .setDate(expectedDate1)
        .build(),
      Price
        .newBuilder()
        .setValue(Int32Value.of(20034))
        .setDate(expectedDate2)
        .build(),
      Price
        .newBuilder()
        .setValue(Int32Value.of(20036))
        .setDate(expectedDate2)
        .build()
    ).asJava

    val expectedIsoPrices: util.List[Price] = Seq(
      Price
        .newBuilder()
        .setValue(Int32Value.of(10014))
        .setDate(expectedDate1)
        .build(),
      Price
        .newBuilder()
        .setValue(Int32Value.of(10016))
        .setDate(expectedDate1)
        .build(),
      Price
        .newBuilder()
        .setValue(Int32Value.of(20014))
        .setDate(expectedDate2)
        .build(),
      Price
        .newBuilder()
        .setValue(Int32Value.of(20016))
        .setDate(expectedDate2)
        .build()
    ).asJava

    val expectedDistrictPrices: util.List[Price] = Seq(
      Price
        .newBuilder()
        .setValue(Int32Value.of(10024))
        .setDate(expectedDate1)
        .build(),
      Price
        .newBuilder()
        .setValue(Int32Value.of(10026))
        .setDate(expectedDate1)
        .build(),
      Price
        .newBuilder()
        .setValue(Int32Value.of(20024))
        .setDate(expectedDate2)
        .build(),
      Price
        .newBuilder()
        .setValue(Int32Value.of(20026))
        .setDate(expectedDate2)
        .build()
    ).asJava

    val expectedPriceDynamics: PriceDynamics =
      PriceDynamics
        .newBuilder()
        .addAllBuilding(expectedBuildingPrices)
        .addAllDistrict(expectedDistrictPrices)
        .addAllFifteenMin(expectedIsoPrices)
        .build()

    val expectedPriceRange: PriceRange =
      PriceRange
        .newBuilder()
        .setMin(samplePredictedPrice.getQ05)
        .setMax(samplePredictedPrice.getQ95)
        .setPercentile25(samplePredictedPrice.getQ25)
        .setPercentile75(samplePredictedPrice.getQ75)
        .setMedian(samplePredictedPrice.getValue)
        .setCurrentBuildingPrice(samplePredictedPrice.getValue)
        .build()

    val expectedPriceStatistic: PriceStatistic =
      PriceStatistic
        .newBuilder()
        .setDynamics(expectedPriceDynamics)
        .setRange(expectedPriceRange)
        .build()

    val expectedPriceStatisticResponse: PriceStatisticResponse =
      PriceStatisticResponse
        .newBuilder()
        .setResponse(expectedPriceStatistic)
        .build()
  }
}
