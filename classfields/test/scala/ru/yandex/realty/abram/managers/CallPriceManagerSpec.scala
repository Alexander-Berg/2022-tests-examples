package ru.yandex.realty.abram.managers

import com.google.protobuf.Timestamp
import com.lucidchart.open.xtract.XmlReader
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.TableFor3
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.abram.policy.{PriceItem, PriceLists}
import ru.yandex.realty.abram.proto.api.call.prices.{CallPriceRequest, CallPriceResponse}
import ru.yandex.realty.clients.seller.SellerClient
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.context.v2.price.list.CallPriceListStorage
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.storage.CampaignHeadersStorage
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.{Cost, Good}

import java.time.Instant
import scala.collection.mutable
import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class CallPriceManagerSpec
  extends AsyncSpecBase
  with FeaturesStubComponent
  with RegionGraphTestComponents
  with ScalaCheckPropertyChecks {

  private lazy val table = initTestData()

  def getPriceLists(priceLists: Seq[String]): PriceLists = {
    PriceLists(priceLists.map(getPriceList).flatMap(_.priceLists))
  }

  private def getPriceList(priceListFile: String) = {
    val xmlFile = getClass.getResourceAsStream(priceListFile)
    val prices = XML.load(xmlFile)
    val reader = XmlReader.of[PriceLists]
    reader.read(prices).toOption.getOrElse {
      throw new IllegalArgumentException("Unable to parse call price list")
    }
  }

  lazy val priceLists: PriceLists = getPriceLists(
    Seq(
      "/call_price_lists_maximum_v9.xml",
      "/call_price_lists_extended_v9.xml",
      "/call_price_lists_minimum_v9.xml"
    )
  )

  "CallPriceManager" should {

    "checking for classes in call prices lists" in {
      priceLists.priceLists.flatMap(_.priceList).exists { priceItem =>
        val pr = (priceItem.priceMaxExclusive.isDefined || priceItem.priceMinInclusive.isDefined) && priceItem.`class`.isEmpty
        if (pr) {
          println(priceItem.toString)
        }
        pr
      } shouldEqual false
    }

    forAll(table) { (tariff: String, tag: String, expected: Long) =>
      s"check for $tariff and tag $tag and price $expected" in {

        val callPriceListStorage = CallPriceListStorage(Map.empty)
        val campaignHeadersStorage = new CampaignHeadersStorage(Iterable.empty)
        val vosClient = mock[VosClientNG]
        val sellerClient = mock[SellerClient]
        val manager = new CallPriceManager(
          regionGraphProvider,
          () => callPriceListStorage,
          () => campaignHeadersStorage,
          vosClient,
          sellerClient,
          features
        )

        val costConstraint = Cost.Constraints.newBuilder().setCostType(Model.CostType.COSTPERCALL)
        val costPerCall = Cost.PerCall.newBuilder().setUnits(1).setConstraints(costConstraint)
        val cost = Model.Cost.newBuilder().setVersion(1).setPerCall(costPerCall)
        val goodCustom = Good.Custom.newBuilder().setId(tariff).setCost(cost)
        val modelGood = Model.Good.newBuilder().setVersion(1).setCustom(goodCustom)
        val modelProduct = Model.Product.newBuilder().setVersion(1).addGoods(modelGood)

        val callPriceRequest = CallPriceRequest
          .newBuilder()
          .setTeleponyTag(tag)
          .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond))
          .setProduct(modelProduct)
          .build()

        val value: CallPriceResponse = manager.getPrice(callPriceRequest).futureValue
        value.getPrice.getBasePrice / 100 shouldBe expected
      }
    }
  }

  private def initTestData() = {

    val distinctPrices = mutable.Map[String, Tuple3[String, String, Long]]()
    priceLists.priceLists
      .foreach(priceList => {
        priceList.priceList
          .flatMap(item => toRgidSeq(item).map(id => (id, item)))
          .foreach {
            case (rgid, item) =>
              val tariff = if (priceList.id.startsWith("maximum")) {
                "Maximum"
              } else if (priceList.id.startsWith("extended")) {
                "TUZ"
              } else {
                "CallsMinimum"
              }

              var tag = s"tuzParamRgid=$rgid#tuzParamType=${item.`type`}"

              item.category.foreach(category => tag += s"#tuzParamCategory=$category")
              item.`class`.foreach(clazz => tag += s"#tuzParamClass=$clazz")
              item.reassignment match {
                case Some(true) => tag += s"#tuzParamReassignment=yes"
                case _ =>
              }
              item.rentTime.foreach(time => tag += s"#tuzParamRentTime=$time")

              val expectedPrice = if (rgid != NodeRgid.CRIMEA) {
                item.price
              } else {
                0
              }
              val key = tariff + tag
              if (!distinctPrices.contains(key)) {
                distinctPrices.put(key, Tuple3(tariff, tag, expectedPrice))
              }
          }
      })

    val seq: Seq[(String, String, Long)] = distinctPrices.values.toSeq.sortBy(tuple => tuple._1 + tuple._2)
    new TableFor3[String, String, Long](("tariff", "tag", "expected"), seq: _*)
  }

  private def toRgidSeq(item: PriceItem) = {
    val geoId = item.geoId
    val rgid = if (geoId == Regions.MSK_AND_MOS_OBLAST) {
      Seq(NodeRgid.MOSCOW_AND_MOS_OBLAST)
    } else if (geoId == Regions.SPB_AND_LEN_OBLAST) {
      Seq(NodeRgid.SPB_AND_LEN_OBLAST)
    } else if (geoId == Regions.KRASNODARSKYJ_KRAI) {
      Seq(NodeRgid.KRASNODARSKYJ_KRAI)
    } else if (geoId == Regions.TATARSTAN) {
      Seq(NodeRgid.TATARSTAN)
    } else if (geoId == Regions.RUSSIA) {
      Seq(NodeRgid.SVERDLOVSKAYA_OBLAST, NodeRgid.CRIMEA)
    } else {
      Seq(geoId)
    }
    rgid
  }
}
