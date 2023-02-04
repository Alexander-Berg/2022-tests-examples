package ru.auto.salesman.test.model.gens

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.ApiOfferModel.OfferStatus.ACTIVE
import ru.auto.api.ApiOfferModel.Section.NEW
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CatalogModel.{Mark, Model}
import ru.auto.api.CommonModel.{GeoPoint, PriceInfo, RegionInfo}
import ru.auto.api.MotoModel.MotoCategory
import ru.auto.api.ResponseModel.OfferListingResponse
import ru.auto.api.TrucksModel.TruckCategory
import ru.auto.api._
import ru.auto.salesman.model.OfferCurrency
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.yandex.vertis.generators.{
  BasicGenerators,
  DateTimeGenerators,
  ProtobufGenerators
}

import scala.collection.JavaConverters.seqAsJavaListConverter

trait OfferModelGenerators
    extends BasicGenerators
    with ProtobufGenerators
    with DateTimeGenerators
    with BasicSalesmanGenerators {

  val OfferStatusGen: Gen[ApiOfferModel.OfferStatus] =
    protoEnum(ApiOfferModel.OfferStatus.values().toSeq)

  val OfferStatusNotDraftGen: Gen[ApiOfferModel.OfferStatus] =
    protoEnum(
      ApiOfferModel.OfferStatus.values().filter(_ != OfferStatus.DRAFT).toSeq
    )

  def offerCategoryFilteredGen(f: Category => Boolean): Gen[Category] =
    protoEnum(
      ApiOfferModel.Category
        .values()
        .filter(f)
        .toSeq
    )

  val OfferCategoryGen: Gen[ApiOfferModel.Category] =
    offerCategoryFilteredGen(_ => true)

  val OfferCategoryKnownGen: Gen[ApiOfferModel.Category] =
    offerCategoryFilteredGen(cat => cat != CATEGORY_UNKNOWN && cat != UNRECOGNIZED)

  def offerSectionFilteredGen(f: Section => Boolean): Gen[Section] =
    protoEnum(
      ApiOfferModel.Section
        .values()
        .filter(f)
        .toSeq
    )

  val OfferSectionGen: Gen[ApiOfferModel.Section] =
    offerSectionFilteredGen(_ => true)

  val OfferSectionKnownGen: Gen[ApiOfferModel.Section] =
    offerSectionFilteredGen { s =>
      s != Section.SECTION_UNKNOWN && s != Section.UNRECOGNIZED
    }

  val NotActiveOfferGen: Gen[ApiOfferModel.Offer] =
    protoEnum(
      ApiOfferModel.OfferStatus
        .values()
        .toSeq
        .filterNot(_ == ACTIVE)
    ).map { status =>
      ApiOfferModel.Offer.newBuilder().setStatus(status).build()
    }

  val ActiveOfferGen: Gen[ApiOfferModel.Offer] =
    Gen.const(ApiOfferModel.Offer.newBuilder().setStatus(ACTIVE).build())

  val OfferWithGeobaseIdGen: Gen[ApiOfferModel.Offer] = for {
    geobaseId <- Gen.posNum[Long]
    location = ApiOfferModel.Location.newBuilder().setGeobaseId(geobaseId)
    seller = ApiOfferModel.Seller.newBuilder.setLocation(location)
  } yield ApiOfferModel.Offer.newBuilder().setSeller(seller).build()

  def offersListingResponseGen(
      f: ApiOfferModel.Offer => Boolean
  ): Gen[OfferListingResponse] =
    for {
      offers <- Gen.listOf(offerGen()).map(_.filter(f))
    } yield OfferListingResponse.newBuilder().addAllOffers(offers.asJava).build

  def offersListingResponseWithOrWithoutOfferGen(
      offerId: String,
      offersWereActivated: Boolean,
      includeOfferWithGivenId: Boolean = false
  ): Gen[OfferListingResponse] = {
    val customOfferGen = offerGen(
      additionalInfoGen = additionalInfoGen(wasActive = Some(offersWereActivated)),
      offerIdGen = autoruOfferIdExcludeGen(Seq(AutoruOfferId(offerId)))
    )
    for {
      offers <- Gen.listOf(customOfferGen)
      offer <-
        if (includeOfferWithGivenId)
          offerGen(
            additionalInfoGen = additionalInfoGen(wasActive = Some(offersWereActivated)),
            offerIdGen = Gen.const(AutoruOfferId(offerId))
          )
        else customOfferGen
    } yield
      OfferListingResponse
        .newBuilder()
        .addAllOffers(offers.asJava)
        .addOffers(offer)
        .build
  }

  private def prefixedLongGen(prefix: String) =
    Gen.posNum[Long].map(prefix + _)

  val dealerRefGen: Gen[String] = prefixedLongGen("dealer:")

  def additionalInfoGen(
      wasActive: Option[Boolean] = None
  ): Gen[AdditionalInfo] =
    for {
      wasActiveGenerated <- bool
    } yield
      AdditionalInfo
        .newBuilder()
        .setWasActive(wasActive.getOrElse(wasActiveGenerated))
        .build()

  val AdditionalInfoGen: Gen[AdditionalInfo] =
    additionalInfoGen(wasActive = None)

  val userRefGen: Gen[String] = prefixedLongGen("user:")

  def carInfoGen(
      markGen: Gen[Mark] = markGen,
      modelGen: Gen[Model] = modelGen
  ): Gen[CarInfo] =
    for {
      mark <- markGen
      model <- modelGen
    } yield CarInfo.newBuilder().setMark(mark.getName).setModel(model.getName).build()

  def locationGen(regionIdGen: Gen[Long] = Gen.posNum[Long]): Gen[Location] =
    for {
      regionId <- regionIdGen
    } yield
      Location
        .newBuilder()
        .setRegionInfo(RegionInfo.newBuilder().setId(regionId))
        .build()

  def LuxuryMoscowCarsNewOfferGen: Gen[ApiOfferModel.Offer] =
    OfferModelGenerators.offerGen(
      offerCategoryGen = CARS,
      offerSectionGen = NEW,
      CarInfoGen = OfferModelGenerators.luxuryCarInfoGen(),
      SellerGen = OfferModelGenerators.sellerGen(
        LocationGen = Location
          .newBuilder()
          .setRegionInfo(
            RegionInfo.newBuilder().setId(1L).build()
          )
          .build()
      )
    )

  def luxuryCarInfoGen() =
    for {
      mark <- Gen.oneOf(
        Seq(
          "ASTON_MARTIN",
          "BENTLEY",
          "FERRARI",
          "LAMBORGHINI",
          "MASERATI",
          "ROLLS_ROYCE",
          "TESLA"
        )
      )
    } yield CarInfo.newBuilder().setMark(mark).build()

  def sellerGen(LocationGen: Gen[Location] = locationGen()): Gen[Seller] =
    for {
      location <- LocationGen
    } yield Seller.newBuilder().setLocation(location).build()

  def priceInfoGen(
      priceGen: Gen[Double] = Gen.posNum[Double],
      currencyGen: Gen[OfferCurrency] = OfferCurrencyGen
  ): Gen[PriceInfo] =
    for {
      price <- priceGen
      currency <- currencyGen
      createTimestamp <- Gen.posNum[Long]
    } yield
      PriceInfo
        .newBuilder()
        .setPrice(price.floatValue())
        .setDprice(price)
        .setCurrency(currency.toString)
        .setCreateTimestamp(createTimestamp)
        .build()

  val motoCategoryGen: Gen[MotoCategory] = protoEnum(
    MotoCategory.values().toSeq
  )

  val truckCategoryGen: Gen[TruckCategory] = protoEnum(
    TruckCategory.values().toSeq
  )

  def offerGen(
      offerIdGen: Gen[AutoruOfferId] = AutoruOfferIdGen,
      offerCategoryGen: Gen[ApiOfferModel.Category] = OfferCategoryGen,
      motoCategoryGen: Gen[MotoCategory] = motoCategoryGen,
      truckCategoryGen: Gen[TruckCategory] = truckCategoryGen,
      offerSectionGen: Gen[ApiOfferModel.Section] = OfferSectionGen,
      statusGen: Gen[ApiOfferModel.OfferStatus] = Gen.const(ACTIVE),
      userRefGen: Gen[String] = dealerRefGen,
      PriceInfoGen: Gen[PriceInfo] = priceInfoGen(),
      CarInfoGen: Gen[CarInfo] = carInfoGen(),
      SellerGen: Gen[Seller] = sellerGen(),
      additionalInfoGen: Gen[AdditionalInfo] = AdditionalInfoGen,
      deliveryInfoGen: Gen[DeliveryInfo] = deliveryInfoGen
  ): Gen[ApiOfferModel.Offer] =
    for {
      id <- offerIdGen
      offerCategory <- offerCategoryGen
      offerSection <- offerSectionGen
      status <- statusGen
      deliveryInfo <- deliveryInfoGen
      userRef <- userRefGen
      motoCategory <- motoCategoryGen
      truckCategory <- truckCategoryGen
      priceInfo <- PriceInfoGen
      carInfo <- CarInfoGen
      seller <- SellerGen
      additionalInfo <- additionalInfoGen
    } yield {
      val builder = ApiOfferModel.Offer
        .newBuilder()
        .setId(id.value)
        .setCategory(offerCategory)
        .setSection(offerSection)
        .setStatus(status)
        .setUserRef(userRef)
        .setPriceInfo(priceInfo)
        .setDeliveryInfo(deliveryInfo)
        .setCarInfo(carInfo)
        .setSeller(seller)
        .setAdditionalInfo(additionalInfo)
      offerCategory match {
        case MOTO => builder.getMotoInfoBuilder.setMotoCategory(motoCategory)
        case TRUCKS =>
          builder.getTruckInfoBuilder.setTruckCategory(truckCategory)
        case _ =>
      }
      builder.build()
    }

  def modelGen: Gen[Model] =
    for {
      code <- nonEmptyStringGen
      name <- nonEmptyStringGen
      ruName <- nonEmptyStringGen
    } yield Model.newBuilder.setCode(code).setName(name).setRuName(ruName).build

  def markGen: Gen[Mark] =
    for {
      code <- nonEmptyStringGen
      name <- nonEmptyStringGen
      ruName <- nonEmptyStringGen
    } yield Mark.newBuilder.setCode(code).setName(name).setRuName(ruName).build

  def carInfoGen: Gen[CarsModel.CarInfo] =
    for {
      mark <- markGen
      model <- modelGen
      techParamId <- Gen.posNum[Long]
      superGenId <- Gen.posNum[Long]
    } yield
      CarsModel.CarInfo.newBuilder
        .setMarkInfo(mark)
        .setModelInfo(model)
        .setTechParamId(techParamId)
        .setSuperGenId(superGenId)
        .build

  def motoInfoGen: Gen[MotoModel.MotoInfo] =
    for {
      mark <- markGen
      model <- modelGen
    } yield MotoModel.MotoInfo.newBuilder.setMarkInfo(mark).setModelInfo(model).build

  def truckInfoGen: Gen[TrucksModel.TruckInfo] =
    for {
      mark <- markGen
      model <- modelGen
    } yield
      TrucksModel.TruckInfo.newBuilder
        .setModelInfo(model)
        .setMarkInfo(mark)
        .build

  def documentsGen: Gen[ApiOfferModel.Documents] =
    for {
      year <- Gen.posNum[Int]
      vin <- nonEmptyStringGen
    } yield ApiOfferModel.Documents.newBuilder.setYear(year).setVin(vin).build

  def offerGenWithMarkModel(
      offerCategoryGen: Gen[ApiOfferModel.Category]
  ): Gen[ApiOfferModel.Offer] =
    for {
      offerId <- AutoruOfferIdGen
      offerCategory <- offerCategoryGen
      carInfo <- carInfoGen
      motoInfo <- motoInfoGen
      truckInfo <- truckInfoGen
      documents <- documentsGen
    } yield {
      val builder = ApiOfferModel.Offer
        .newBuilder()
        .setId(offerId.toString)
        .setCategory(offerCategory)
        .setDocuments(documents)
      offerCategory match {
        case ApiOfferModel.Category.CARS =>
          builder.setCarInfo(carInfo)
        case ApiOfferModel.Category.MOTO =>
          builder.setMotoInfo(motoInfo)
        case ApiOfferModel.Category.TRUCKS =>
          builder.setTruckInfo(truckInfo)
        case _ =>
      }
      builder.build
    }

  def deliveryInfoGen: Gen[DeliveryInfo] =
    for {
      n <- Gen.choose(1, 10)
      deliveryRegions <- Gen.listOfN(n, deliveryRegionGen)
    } yield
      DeliveryInfo
        .newBuilder()
        .addAllDeliveryRegions(deliveryRegions.asJava)
        .build()

  private def deliveryRegionGen: Gen[DeliveryRegion] =
    for {
      geobaseId <- Gen.choose(1, 1000000)
      latitude <- Gen.choose(1, 10000)
      longitude <- Gen.choose(1, 10000)
      address <- Gen.alphaStr
      federalSubjectId <- Gen.choose(1, 1000000)
      cityName <- Gen.alphaStr
      cityId <- Gen.choose(1, 1000000)
    } yield
      DeliveryRegion
        .newBuilder()
        .setLocation(
          Location
            .newBuilder()
            .setAddress(address)
            .setGeobaseId(geobaseId)
            .setFederalSubjectId(federalSubjectId)
            .setRegionInfo(
              RegionInfo
                .newBuilder()
                .setId(cityId)
                .setName(cityName)
            )
            .setCoord(
              GeoPoint
                .newBuilder()
                .setLatitude(latitude)
                .setLongitude(longitude)
            )
        )
        .build()

  def paidServiceGen: Gen[CommonModel.PaidService] =
    for {
      product <- ProductIdGen
      isActive <- bool
      createDate <- dateTimeInPast
      expireDate <- dateTimeInFuture()
      badge <- Gen.option(readableString)
    } yield {
      val b = CommonModel.PaidService
        .newBuilder()
        .setService(product.toString)
        .setIsActive(isActive)
        .setCreateDate(createDate.getMillis)
        .setExpireDate(expireDate.getMillis)
      badge.foreach(b.setBadge)
      b.build()
    }

  def addServicesRequestGen: Gen[RequestModel.AddServicesRequest] =
    for {
      services <- listUnique(1, 3, paidServiceGen)(_.getService)
    } yield
      RequestModel.AddServicesRequest
        .newBuilder()
        .addAllServices(services.asJava)
        .build()
}

object OfferModelGenerators extends OfferModelGenerators
