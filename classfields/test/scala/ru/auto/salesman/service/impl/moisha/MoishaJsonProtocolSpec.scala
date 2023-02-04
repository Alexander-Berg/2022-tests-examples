package ru.auto.salesman.service.impl.moisha

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.model.{OfferCategories, ProductId, RegionId, TariffTypes}
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.PriceEstimateService.PriceRequest.{
  ClientOffer,
  OffersHistoryReportsContext,
  QuotaContext,
  SubscriptionOffer,
  UserOffer
}
import ru.auto.salesman.service.impl.moisha.MoishaJsonProtocol._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.{DateTimeInterval, PriceRequestContextType}
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue}

import scala.language.implicitConversions

class MoishaJsonProtocolSpec extends BaseSpec {

  private val price = 1000000
  private val createDateStr = "2018-09-17T22:22:00.000+03:00"
  private val createDate = DateTime.parse(createDateStr)
  private val mark = "AUDI"
  private val model = "A3"
  private val generation = "4327893"
  private val year = 2017
  private val geoId = RegionId(213)

  "ProductIdFormat" should {
    import ProductId._

    val excludeProducts = List(ProductId.Add)

    "correctly convert AutoRu products to Moisha" in {

      ProductId.values.filterNot(excludeProducts.contains(_)).foreach { p =>
        check(ProductIdFormat.write(p), alias(p))
      }
    }

    "correctly convert Moisha products to AutoRu" in {
      ProductId.values.filterNot(excludeProducts.contains(_)).foreach { p =>
        ProductIdFormat.read(alias(p)) should be(p)
      }
    }

    "fail on excluded product" in {
      intercept[NoSuchElementException] {
        ProductIdFormat.write(excludeProducts.head)
      }
    }

    "convert and parse all AutoRu products to/from Moisha" in {
      val WithoutExcluded =
        ProductId.values.filterNot(excludeProducts.contains(_))

      val autoRuAsMoisha =
        WithoutExcluded.map(ProductIdFormat.write).map(asString)
      autoRuAsMoisha.map(ProductId.withAlias)

      val read = autoRuAsMoisha.map(p => ProductIdFormat.read(asJson(p)))
      read should be(WithoutExcluded)
    }

    implicit def asJson(product: String): JsString = JsString(product)

    def asString(jsString: JsValue): String = jsString.convertTo[String]

    def check(actual: JsValue, expected: String): Unit =
      asString(actual) shouldBe expected
  }

  "ContextWriter" should {

    "convert and parse all AutoRu products to/from Moisha" in {
      val clientRegionId = RegionId(10174L)
      val amount = 2147483647
      val marks = List("OPEL", "BMW")
      val tariffType = TariffTypes.LuxaryMsk

      val quotaContext =
        QuotaContext(clientRegionId, marks, amount, Some(tariffType))
      val jsonString = ContextWriter.write(quotaContext)

      jsonString shouldBe JsObject(
        "clientRegionId" -> JsNumber(clientRegionId),
        "clientMarks" -> JsArray(JsString("OPEL"), JsString("BMW")),
        "amount" -> JsNumber(amount),
        "tariff" -> JsString(tariffType.toString)
      )
    }
  }

  "OfferWriter" should {

    "write cars client offer" in {
      val category = OfferCategories.Cars
      val section = Section.USED
      val offer =
        ClientOffer(price, createDate, category, section, mark, model)
      OfferWriter.write(offer) shouldBe JsObject(
        "price" -> JsNumber(price),
        "creationTs" -> JsString(createDateStr),
        "transport" -> JsString("cars"),
        "category" -> JsString("used"),
        "mark" -> JsString("AUDI"),
        "model" -> JsString("A3")
      )
    }

    "write atv client offer" in {
      val category = OfferCategories.Atv
      val section = Section.NEW
      val offer =
        ClientOffer(price, createDate, category, section, mark, model)
      OfferWriter.write(offer) shouldBe JsObject(
        "price" -> JsNumber(price),
        "creationTs" -> JsString(createDateStr),
        "transport" -> JsString("atv"),
        "category" -> JsString("new"),
        "mark" -> JsString("AUDI"),
        "model" -> JsString("A3")
      )
    }

    "write cars user offer" in {
      val category = OfferCategories.Cars
      val section = Section.USED
      val offer = UserOffer(
        category,
        section,
        mark,
        model,
        Some(generation),
        year,
        Seq(geoId),
        price,
        createDate
      )
      OfferWriter.write(offer) shouldBe JsObject(
        "category" -> JsString("cars"),
        "section" -> JsString("used"),
        "mark" -> JsString(mark),
        "model" -> JsString(model),
        "generation" -> JsString(generation),
        "year" -> JsNumber(year),
        "geoId" -> JsArray(JsNumber(geoId)),
        "price" -> JsNumber(price),
        "creationTs" -> JsString(createDateStr)
      )
    }

    "write atv user offer" in {
      val category = OfferCategories.Atv
      val section = Section.NEW
      val offer = UserOffer(
        category,
        section,
        mark,
        model,
        Some(generation),
        year,
        Seq(geoId),
        price,
        createDate
      )
      OfferWriter.write(offer) shouldBe JsObject(
        "category" -> JsString("atv"),
        "section" -> JsString("new"),
        "mark" -> JsString(mark),
        "model" -> JsString(model),
        "generation" -> JsString(generation),
        "year" -> JsNumber(year),
        "geoId" -> JsArray(JsNumber(geoId)),
        "price" -> JsNumber(price),
        "creationTs" -> JsString(createDateStr)
      )
    }

    "write cars subscription offer" in {
      val category = OfferCategories.Cars
      val section = Section.USED
      val offer = SubscriptionOffer(
        category,
        section,
        Some(mark),
        Some(model),
        Some(generation),
        Some(year),
        Seq(geoId),
        Some(price)
      )
      OfferWriter.write(offer) shouldBe JsObject(
        "category" -> JsString("cars"),
        "section" -> JsString("used"),
        "mark" -> JsString(mark),
        "model" -> JsString(model),
        "generation" -> JsString(generation),
        "year" -> JsNumber(year),
        "geoId" -> JsArray(JsNumber(geoId)),
        "price" -> JsNumber(price)
      )
    }

    "write atv subscription offer" in {
      val category = OfferCategories.Atv
      val section = Section.NEW
      val offer = SubscriptionOffer(
        category,
        section,
        Some(mark),
        Some(model),
        Some(generation),
        Some(year),
        Seq(geoId),
        Some(price)
      )
      OfferWriter.write(offer) shouldBe JsObject(
        "category" -> JsString("atv"),
        "section" -> JsString("new"),
        "mark" -> JsString(mark),
        "model" -> JsString(model),
        "generation" -> JsString(generation),
        "year" -> JsNumber(year),
        "geoId" -> JsArray(JsNumber(geoId)),
        "price" -> JsNumber(price)
      )
    }
  }

  "RequestWriter" should {

    val interval =
      DateTimeInterval.wholeDay(DateTime.parse("2020-07-27T10:27:00+03:00"))

    def vinHistoryReportRequest(offer: Option[SubscriptionOffer]) = {
      val context = OffersHistoryReportsContext(
        reportsCount = 1,
        contextType = Some(PriceRequestContextType.VinHistory),
        geoId = None,
        contentQuality = None,
        experiment = None
      )
      PriceRequest(
        offer,
        context,
        ProductId.OffersHistoryReports,
        interval,
        None
      )
    }

    "write offer from offers-history-reports request into offer field" in {
      val category = OfferCategories.Cars
      val section = Section.NEW
      val offer = SubscriptionOffer(
        category,
        section,
        Some(mark),
        Some(model),
        Some(generation),
        Some(year),
        Seq(geoId),
        Some(price)
      )
      val request = vinHistoryReportRequest(Some(offer))
      val result = MoishaRequestWriter.write(request)
      result.asJsObject.fields("offer").asJsObject shouldBe JsObject(
        "category" -> JsString("cars"),
        "section" -> JsString("new"),
        "mark" -> JsString(mark),
        "model" -> JsString(model),
        "generation" -> JsString(generation),
        "year" -> JsNumber(year),
        "geoId" -> JsArray(JsNumber(geoId)),
        "price" -> JsNumber(price)
      )
    }

    "leave offer field empty for offers-history-reports request without offer" in {
      val request = vinHistoryReportRequest(offer = None)
      val result = MoishaRequestWriter.write(request)
      result.asJsObject.fields.get("offer") shouldBe empty
    }

    "fill context field for offers-history-reports request" in {
      val request = vinHistoryReportRequest(offer = None)
      val result = MoishaRequestWriter.write(request)
      result.asJsObject.fields("context") shouldBe JsObject(
        "reportsCount" -> JsNumber(1),
        "contextType" -> JsString("VIN_HISTORY")
      )
    }

    "fill product field for offers-history-reports request" in {
      val request = vinHistoryReportRequest(offer = None)
      val result = MoishaRequestWriter.write(request)
      result.asJsObject.fields("product") shouldBe JsString(
        "offers-history-reports"
      )
    }

    "fill interval field for offers-history-reports request" in {
      val request = vinHistoryReportRequest(offer = None)
      val result = MoishaRequestWriter.write(request)
      result.asJsObject.fields("interval") shouldBe JsObject(
        "from" -> JsString("2020-07-27T00:00:00.000+03:00"),
        "to" -> JsString("2020-07-27T23:59:59.999+03:00")
      )
    }
  }
}
