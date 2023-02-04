package ru.yandex.realty.service.builder

import com.google.protobuf.Timestamp
import eu.timepit.refined.auto._
import org.junit.runner.RunWith
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.unified.offer.offercategory.ApartmentCategory
import ru.yandex.realty.proto.unified.offer.offertype.SellOffer
import ru.yandex.realty.proto.unified.offer.price.TransactionInfo
import ru.yandex.realty.service.builder.MicroAdBuilder.BuildError
import ru.yandex.realty.service.builder.live.OfferMicroAdBuilder
import ru.yandex.realty.traffic.model.ad.{MicroAd, MicroAdData, MicroAdRelevance}
import zio._
import zio.test.Assertion._
import zio.test._
import zio.test.junit._

import java.util.zip.GZIPInputStream

@RunWith(classOf[ZTestJUnitRunner])
class OfferMicroAdBuilderSpec extends JUnitRunnableSpec {

  import OfferMicroAdBuilderSpecTestData._

  private def serviceLayer = {
    ZLayer.succeed(mdsUrlBuilder) >>> OfferMicroAdBuilder.live
  }

  private def testMissingResult(field: String)(offer: UnifiedOffer) =
    testM(s"when no $field") {
      assertM {
        ZIO
          .service[MicroAdBuilder.Service[UnifiedOffer]]
          .flatMap(_.buildMicroAd(offer, MicroAdRelevance(1.0d, 0.0d)))
          .run
      }(fails(equalTo(BuildError.MissingValue(field))))
    }

  private def defaultTestMissingResult(field: String)(offer: UnifiedOffer) =
    testMissingResult(field)(offer)
      .provideLayer(serviceLayer)

  private def missingSuite =
    suite("Return missing error")(
      defaultTestMissingResult("type") {
        UnifiedOffer
          .newBuilder()
          .setOfferId("1")
          .build()
      },
      defaultTestMissingResult("update_time") {
        UnifiedOffer
          .newBuilder()
          .setOfferId("1")
          .setSell(SellOffer.getDefaultInstance)
          .build()
      },
      defaultTestMissingResult("category") {
        UnifiedOffer
          .newBuilder()
          .setOfferId("1")
          .setSell(SellOffer.getDefaultInstance)
          .setUpdateTime(Timestamp.newBuilder().setSeconds(1000))
          .build()
      },
      defaultTestMissingResult("price") {
        UnifiedOffer
          .newBuilder()
          .setOfferId("1")
          .setSell(SellOffer.getDefaultInstance)
          .setUpdateTime(Timestamp.newBuilder().setSeconds(1000))
          .setApartment(ApartmentCategory.getDefaultInstance)
          .build()
      },
      defaultTestMissingResult("image_url") {
        UnifiedOffer
          .newBuilder()
          .setOfferId("1")
          .setSell(SellOffer.getDefaultInstance)
          .setUpdateTime(Timestamp.newBuilder().setSeconds(1000))
          .setApartment(ApartmentCategory.getDefaultInstance)
          .setTransactionInfo(TransactionInfo.newBuilder().setWholeOfferPriceRub(100))
          .build()
      }
    )

  private def hasTitle(title: String) =
    hasField[MicroAd, String](
      "title",
      _.data.asInstanceOf[MicroAdData.OfferData].title,
      equalTo(title)
    )

  private def defaultTitleRenderTest(offer: UnifiedOffer)(expected: String) =
    testM(s"`$expected`") {
      assertM {
        ZIO
          .service[MicroAdBuilder.Service[UnifiedOffer]]
          .flatMap(_.buildMicroAd(offer, MicroAdRelevance(1.0d, 0.0d)))
          .provideLayer(serviceLayer)
          .run
      }(succeeds(hasTitle(expected)))
    }

  private def titleRenderSuite =
    suite("correctly render expected")(
      defaultTitleRenderTest(readOffer(OneRoomOffer))("1-комнатная, 55 м²"),
      defaultTitleRenderTest(readOffer(StudioOffer))("Студия, 100 м²"),
      defaultTitleRenderTest(readOffer(_7RoomOffer))("7-комнатная, 88 м²"),
      defaultTitleRenderTest(readOffer(RoomOffer))("Комната 11 м²"),
      defaultTitleRenderTest(readOffer(HouseOffer))("Дом 100 м², участок 0.8 соток"),
      defaultTitleRenderTest(readOffer(LotOffer))("Участок 201 сотка"),
      defaultTitleRenderTest(readOffer(OfficeOffer))("Офис, 100 м²"),
      defaultTitleRenderTest(readOffer(LandOffer))("Участок коммерческого назначения, 3 сотки"),
      defaultTitleRenderTest(readOffer(GarageOffer))("Гараж 123 м²"),
      defaultTitleRenderTest(readOffer(CarPlaceOffer))("Машиноместо 12 м²")
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("OfferCostPlusOfferBuilder")(
      missingSuite,
      titleRenderSuite
    )
}

object OfferMicroAdBuilderSpecTestData {
  val OneRoomOffer: String = "4045855157126122752"
  val StudioOffer: String = "4094441573130454619"
  val _7RoomOffer: String = "222452781463533354"
  val RoomOffer: String = "6821634797940226817"
  val HouseOffer: String = "2701383832496513280"
  val LotOffer: String = "3410214229126091521"
  val OfficeOffer: String = "8376440130357640192"
  val LandOffer: String = "4298501888989166081"
  val GarageOffer: String = "7927330541436462848"
  val CarPlaceOffer: String = "7307063186108563712"

  val mdsUrlBuilder: MdsUrlBuilder = new MdsUrlBuilder("//avatars.mds.yandex.net")

  def readOffer(offerId: String): UnifiedOffer = {
    val is = new GZIPInputStream(
      getClass.getClassLoader.getResourceAsStream(s"$offerId.proto.bytes")
    )

    val res = UnifiedOffer.parseFrom(is)
    is.close()
    res
  }
}
