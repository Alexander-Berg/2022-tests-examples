package ru.vertistraf.cost_plus.model

import common.tagged.tag
import ru.vertistraf.common.util.`enum`.ProtoMappedEnumEntry
import ru.vertistraf.common.util.{ProtoMappedEnumSupport, StrictProtoConverter}
import ru.vertistraf.cost_plus.model
import ru.vertistraf.cost_plus.model.ServiceSetInfo.AutoSetInfo
import ru.vertistraf.cost_plus.model.auto.{AutoUrlParams, Filter, Section}
import ru.vertistraf.cost_plus.model.converter.proto._
import ru.vertistraf.cost_plus.model.converter.proto.auto.AutoUrlParamsProtoConverter
import ru.vertistraf.cost_plus.model.converter.proto.result.{
  CostPlusOfferProtoConverter,
  CostPlusPriceProtoConverter,
  CostPlusSetProtoConverter
}
import ru.vertistraf.cost_plus.model.result.CostPlusPrice
import ru.yandex.vertistraf.proto.cost_plus.set.set.AutoUrlParamsMessage
import scalapb.{GeneratedEnum, GeneratedMessage}
import zio.prelude.NonEmptyList
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}

object ProtoConvertersSpec extends DefaultRunnableSpec {

  import ServiceOffer._
  import model.result

  private def simpleTest[A, M <: GeneratedMessage](converter: StrictProtoConverter[A, M], model1: A, more: A*) = {
    suite(s"converter ${converter.getClass.getCanonicalName}")(
      (Seq(model1) ++ more).map { m =>
        test(s"should correctly convert $m") {
          val actual = converter.fromProto(converter.toProto(m))
          assertTrue(actual == m)
        }
      }: _*
    )
  }

  private lazy val Review = model.Review(1, "/some/url")
  private lazy val CarouselImage = model.CarouselImage("url", "title")
  private lazy val Moto = Auto.Moto(1)
  private lazy val Truck = Auto.Truck(1)
  private lazy val RealtyOffer = Realty.Offer(1)
  private lazy val Site = Realty.Site(1)
  private lazy val Village = Realty.Village(1)
  private lazy val CarouselAuto = ServiceSetInfo.AutoSetInfo.Carousel

  private lazy val ServiceSet = model.ServiceSet("url", "title", CarouselAuto)

  private lazy val Car = Auto.Car(
    relevance = 1,
    offerId = "offer id",
    price = 1000,
    vendor = "vendor",
    offerImage = CarouselImage,
    modelImage = CarouselImage.copy(title = CarouselImage.title + "model"),
    markImage = CarouselImage.copy(title = CarouselImage.title + "mark"),
    dealerImage = Some(CarouselImage.copy(title = CarouselImage.title + "dealer")),
    tableImages = NonEmptyList("img1", "img2"),
    modelReview = Review,
    markReview = Review.copy(url = Review.url + "mark"),
    markUrlCode = "MARK_CODE",
    modelUrlCode = "MODEL_CODE",
    dealerDirectUrl = Some("dealer direct url"),
    superGenId = Some(20034723L),
    techParamId = 20034926L,
    configurationId = 20034925L,
    transmissionName = "transmission name",
    complectationId = Some(20747718L),
    complectationName = Some("complectation name"),
    enginePower = 123,
    engineDisplacementLiters = 1.6,
    utmTerm = Some("utm term")
  )

  private lazy val Id = tag[IdTag][String]("1")
  private lazy val CostPlusSet = result.CostPlusSet("url", "title", Id)
  private lazy val PriceFrom = CostPlusPrice.From(1.0)
  private lazy val PriceDirect = CostPlusPrice.Direct(1.0)

  private lazy val CostPlusOffer = result.CostPlusOffer(
    id = Id,
    name = "name",
    url = "url",
    price = PriceDirect,
    currency = "RUR",
    categoryId = 1,
    pictures = Seq("img1", "img2"),
    vendor = Some("vendor"),
    description = Some("description"),
    params = Map(
      "Ссылка на отзывы" -> "10",
      "param2" -> "abacaba",
      "Коробка передач" -> "transmission name",
      "Двигатель, л.с." -> "123",
      "Двигатель, литры" -> "1.6"
    ),
    sets = Seq(CostPlusSet, CostPlusSet)
  )

  private val AutoParams = AutoUrlParams(
    geoId = Some(10L),
    markCode = Some("MARK"),
    modelCode = Some("MODEL"),
    section = Some(Section.All),
    filters = Set(
      Filter.CatalogFilter,
      Filter.Color
    )
  )

  private val AutoRawServiceSetInfo =
    RawServiceSetInfo.Auto(AutoParams)

  private val RawSet =
    RawServiceSet(urlPath = "url/path", title = "title", info = AutoRawServiceSetInfo)

  private def protoEnumeratumTest[
      M <: GeneratedEnum,
      E <: ProtoMappedEnumEntry[M],
      T <: enumeratum.Enum[E] with ProtoMappedEnumSupport[M, E]
    ](`enum`: T) =
    suite(s"converter ${`enum`.getClass.getCanonicalName}")(
      enum.values
        .map { v =>
          test(s"should correctly convert $v") {
            assertTrue(v == `enum`.fromProto(`enum`.toProto(v)))
          }
        }: _*
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ProtoConverters")(
      simpleTest(ReviewProtoConverter, Review),
      simpleTest(CarouseImageProtoConverter, CarouselImage),
      simpleTest(AutoMotoProtoConverter, Moto),
      simpleTest(AutoTruckProtoConverter, Truck),
      simpleTest(RealtyOfferProtoConverter, RealtyOffer),
      simpleTest(RealtySiteProtoConverter, Site),
      simpleTest(RealtyVillageProtoConverter, Village),
      simpleTest(AutoCarProtoConverter, Car),
      simpleTest(ServiceOfferProtoConverter, Moto, Truck, Car, RealtyOffer, Site, Village),
      simpleTest(
        AutoSetInfoProtoConverter,
        AutoSetInfo.Carousel,
        AutoSetInfo.Collapse.ByModel(None),
        AutoSetInfo.Collapse.ByModel(Some("MODEL")),
        AutoSetInfo.Collapse.Tags(Some("moskva"), Section.All),
        AutoSetInfo.Collapse.Vendor("foreign"),
        AutoSetInfo.Collapse.ByDealer,
        AutoSetInfo.Collapse.ByMark,
        AutoSetInfo.Table.Complectation
      ),
      simpleTest(RawServiceSetInfoProtoConverter, RawServiceSetInfo.Realty, AutoRawServiceSetInfo),
      simpleTest(RawServiceSetProtoConverter, RawSet),
      simpleTest(ServiceSetProtoConverter, ServiceSet),
      simpleTest(AllServiceAdSetsProtoConverter, AllServiceAdSets(Car, Seq(RawSet, RawSet))),
      simpleTest(CostPlusPriceProtoConverter, PriceFrom, PriceDirect),
      simpleTest(CostPlusSetProtoConverter, CostPlusSet),
      simpleTest(CostPlusOfferProtoConverter, CostPlusOffer),
      protoEnumeratumTest[AutoUrlParamsMessage.FilterMessage, Filter, Filter.type](Filter),
      protoEnumeratumTest[AutoUrlParamsMessage.SectionMessage, Section, Section.type](Section),
      simpleTest(AutoUrlParamsProtoConverter, AutoParams)
    )
}
