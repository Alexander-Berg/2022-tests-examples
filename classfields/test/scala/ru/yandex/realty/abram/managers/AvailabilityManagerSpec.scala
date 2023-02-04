package ru.yandex.realty.abram.managers

import java.lang
import java.nio.charset.Charset

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, _}
import ru.yandex.realty.abram.api.ApiExceptionHandler
import ru.yandex.realty.abram.api.v1.prices.PricesHandler
import ru.yandex.realty.abram.application.{AbramConfig, AbramExtDataProviders}
import ru.yandex.realty.abram.managers.AvailabilityManagerSpec._
import ru.yandex.realty.abram.policy.XmlPricesFeed
import ru.yandex.realty.abram.proto.api.ability.{ProductPrice, ProductPriceRequest, ProductPriceResponse}
import ru.yandex.realty.application.ng.DefaultConfigProvider
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.proto.offer.{OfferCategory, OfferType, PaymentType}
import ru.yandex.realty.proto.seller.ProductTypes
import ru.yandex.realty.serialization.json.ProtoJsonFormats

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

@RunWith(classOf[JUnitRunner])
class AvailabilityManagerSpec extends HandlerSpecBase with ProtoJsonFormats with PropertyChecks with Logging {

  val abramConfig = AbramConfig(DefaultConfigProvider.provideForName("abram-test"))
  val abramExtDataProviders = AbramExtDataProviders(abramConfig)
  val vosClientMock = mock[VosClientNG]
  val features = new SimpleFeatures
  val xmlPricesFeed = new XmlPricesFeed(features)

  val availabilityManager =
    new AvailabilityManager(vosClientMock, abramExtDataProviders.regionGraphProvider, features, xmlPricesFeed)(ec)
  val pricesRegionsManager = new PricesRegionsManager()(ec)

  override def routeUnderTest: Route = new PricesHandler(availabilityManager, pricesRegionsManager).route

  override protected def exceptionHandler: ExceptionHandler = ApiExceptionHandler.handler

  override protected def rejectionHandler: RejectionHandler = RejectionHandler.default

  "getProductPrices" should {
    "return correct price" in {
      forAll(offerFeatureGen) { body: ProductPriceRequest =>
        features.ShouldApplyNewPriceUpdate.setNewState(true)
        Post(
          Uri(s"/prices/request"),
          body
        )(marshaller, ec) ~> route ~> check {
          status shouldBe StatusCodes.OK
          val ppr = entityAs[ProductPriceResponse]
          ppr.getProductsList.asScala.size should be > 0
        }
      }
    }
  }

  def offerFeatureGen: Gen[ProductPriceRequest] =
    for {
      offerCategory <- Gen.oneOf(
        OfferCategory.ROOMS,
        OfferCategory.APARTMENT,
        OfferCategory.COMMERCIAL,
        OfferCategory.GARAGE,
        OfferCategory.HOUSE,
        OfferCategory.LOT
      )
      offerType <- Gen.oneOf(
        OfferType.RENT,
        OfferType.SELL
      )
      paymentType <- Gen.oneOf(
        PaymentType.PT_JURIDICAL_PERSON,
        PaymentType.PT_NATURAL_PERSON
      )
      geoId <- Gen.oneOf(
        1, 10174, 10693, // Калужская
        10672, // Воронежская
        11316, // Новосиб
        10995 // Краснодарская
      )
      rentPrice <- Gen.choose(1000, 5099999)
      sellPrice <- Gen.choose(99999, 99999999)
    } yield ProductPriceRequest
      .newBuilder()
      .setRgid(getRgIdByGeoId(geoId).map(_.toInt).getOrElse(throw new Exception(s"Can't find geoId by rgId = $geoId")))
      .setOfferType(offerCategory match {
        case OfferCategory.LOT => OfferType.SELL
        case _ => offerType
      })
      .setCategoryType(offerCategory)
      .setPaymentType(paymentType)
      .setPrice(offerType match {
        case OfferType.RENT if offerCategory != OfferCategory.LOT => rentPrice
        case _ => sellPrice
      })
      .build()

  private def getRgIdByGeoId(geoId: Int): Option[lang.Long] = {
    val regionGraph = abramExtDataProviders.regionGraphProvider.get()
    val region = regionGraph.getNodeByGeoId(geoId)
    log.info(s"Get rgId for geoId=$geoId ${Option(region).flatMap(x => Option(x.getName)).map(_.getDisplay)}")
    if (region != null) Some(region.getId)
    else None
  }
}

case class ProductItem(productType: ProductTypes, basePrice: Int, effectivePrice: Int, periodDays: Int)
case class Products(products: Seq[ProductItem])

object AvailabilityManagerSpec {
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val ex: ExecutionContextExecutor = ExecutionContext.global

  implicit val ProductItemReads: Reads[ProductItem] = (
    (JsPath \ "productType").read[String].map(s => ProductTypes.valueOf("PRODUCT_TYPE_" + s)) and
      (JsPath \ "basePrice").read[String].map(_.toInt) and
      (JsPath \ "effectivePrice").read[String].map(_.toInt) and
      (JsPath \ "periodDays").read[Int]
  )(ProductItem.apply _)
  implicit val ProductsFormat = Json.reads[Products]

  private val mediaTypes: Seq[MediaType.WithFixedCharset] =
    Seq(MediaType.applicationWithFixedCharset("json", HttpCharsets.`UTF-8`, "js"))

  private val unmarshallerContentTypes: Seq[ContentTypeRange] = mediaTypes.map(ContentTypeRange.apply)
  implicit val playJsonUnmarshaller: FromEntityUnmarshaller[ProductPriceResponse] = {
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(unmarshallerContentTypes: _*)
      .map {
        case ByteString.empty =>
          throw Unmarshaller.NoContentException
        case data =>
          val jsString = data.decodeString(Charset.forName("UTF-8"))
          val productsReply = Json.parse(jsString).as[Products]
          val b = ProductPriceResponse.newBuilder()
          productsReply.products.foreach { x =>
            b.addProducts(
              ProductPrice
                .newBuilder()
                .setProductType(x.productType)
                .setBasePrice(x.basePrice)
                .setEffectivePrice(x.effectivePrice)
                .setPeriodDays(x.periodDays)
                .build()
            )
          }
          b.build()
      }
  }
}
