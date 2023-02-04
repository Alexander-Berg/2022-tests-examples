package ru.yandex.vertis.parsing.auto.util

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.doppel.model.proto.Cluster
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.util.RandomUtil.{choose, randomSymbols}
import ru.yandex.vertis.parsing.util.{EnumUtils, RandomUtil}

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._

/**
  * Created by andrey on 2/14/18.
  */
object TestDataUtils {
  private val testId: AtomicInteger = new AtomicInteger(60873378)

  def testDromTrucksUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    "https://spec.drom.ru/ufa/truck/kamaz-6520-ljux-" + offerId + ".html"
  }

  def testDromCarsUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    "https://krasnoyarsk.drom.ru/audi/a4/" + offerId + ".html"
  }

  // -------------------------------------------

  def testDromTrucksPhotoUrl: String = {
    val id = testId.getAndIncrement()
    s"https://static.baza.farpost.ru/v/${id}_bulletin"
  }

  def testDromCarsPhotoUrl: String = {
    val id = testId.getAndIncrement()
    s"https://s.auto.drom.ru/i24221/s/photos/29853/29852756/gen1200_$id.jpg"
  }

  // -------------------------------------------

  def testAvitoTrucksUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    "https://www.avito.ru/taganrog/gruzoviki_i_spetstehnika/prodam_gazel_biznes_" + offerId
  }

  def testAvitoMobileCarsUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    "https://m.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_" + offerId
  }

  def testAvitoCarsUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    "https://www.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_" + offerId
  }

  def toAvitoCarsUrl(avitoMobileCarsUrl: String): String = {
    avitoMobileCarsUrl.replace("https://m.", "http://www.")
  }

  def toAvitoMobileCarsUrl(avitoCarsUrl: String): String = {
    avitoCarsUrl.replace("https://www.", "http://m.")
  }

  // -------------------------------------------

  def testAmruCarsUrl: String = {
    s"https://auto.youla.ru/advert/used/nissan/qashqai/prv--${RandomUtil.nextHexString(16)}"
  }

  // -------------------------------------------

  def testYoulaCarsUrl: String = {
    s"https://youla.io/zelenograd/avto-moto/avtomobili/bmw-e39-${RandomUtil.nextHexString(16)}"
  }

  // -------------------------------------------

  def testE1CarsUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    s"https://auto.e1.ru/car/used/vaz/21927_kalina_ii_hetchbek/$offerId"
  }

  // -------------------------------------------

  def testAuto29CarsUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    s"http://m.auto.29.ru/car/motors/foreign/details/$offerId.php"
  }

  // -------------------------------------------

  def testAutochelCarsUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    s"http://m.autochel.ru/car/motors/foreign/details/$offerId.php"
  }

  // -------------------------------------------

  def testAvitoPhotoUrl: String = {
    val id = testId.getAndIncrement()
    s"https://74.img.avito.st/640x480/$id.jpg"
  }

  def testRow(url: String,
              parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder(),
              category: Category = Category.TRUCKS,
              geobaseId: Long = 0,
              source: CommonModel.Source = CommonModel.Source.HTTP,
              now: DateTime = DateTime.now(),
              doppelClusterSeq: Option[Seq[Cluster]] = None,
              deactivateDate: Option[DateTime] = None,
              sellerUrl: String = ""): ParsedRow = {
    val site = Site.fromUrl(url)
    val remoteId = CommonAutoParser.remoteId(url)
    val hash = CommonAutoParser.hash(url)
    if (EnumUtils.notValidEnumValue(parsedOffer.getOffer.getCategory)) {
      parsedOffer.getOfferBuilder.setCategory(category)
    }
    if (parsedOffer.getOffer.getAdditionalInfo.getRemoteId.isEmpty) {
      parsedOffer.getOfferBuilder.getAdditionalInfoBuilder.setRemoteId(remoteId)
    }
    if (parsedOffer.getOffer.getAdditionalInfo.getRemoteUrl.isEmpty) {
      parsedOffer.getOfferBuilder.getAdditionalInfoBuilder.setRemoteUrl(url)
    }
    if (parsedOffer.getHash.isEmpty) parsedOffer.setHash(hash)
    if (geobaseId != 0) {
      parsedOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(geobaseId)
    }
    if (doppelClusterSeq.isEmpty) {
      parsedOffer
        .addDoppelClusterBuilder()
        .setEnoughData(true)
    } else {
      parsedOffer.addAllDoppelCluster(doppelClusterSeq.get.asJavaCollection)
    }
    if (sellerUrl.nonEmpty) {
      parsedOffer.setSellerUrl(sellerUrl)
    }
    ParsedRow(
      id = 0,
      hash = hash,
      category = category,
      status = CommonModel.Status.NEW,
      site = site,
      url = url,
      data = parsedOffer.build(),
      createDate = now,
      updateDate = now,
      statusUpdateDate = now,
      sentDate = None,
      openDate = None,
      deactivateDate = deactivateDate,
      source = source,
      callCenter = None,
      offerId = None,
      version = 1
    )
  }

  def getRandomPhone: String = {
    "7" + choose(Seq("905", "926", "916")) + randomSymbols(7, ('0', '9'))
  }

  def getRandomStaticPhone: String = {
    "7800" + randomSymbols(7, ('0', '9'))
  }

  def randomLicensePlate: String = {
    val p1 = RandomUtil.choose("АВЕКМНОРСТУХ").toString
    val p2 = RandomUtil.chooseN(3, "0123456789").mkString
    val p3 = RandomUtil.chooseN(2, "АВЕКМНОРСТУХ").mkString
    val p4 = RandomUtil.chooseN(2, "0123456789").mkString
    s"$p1$p2$p3$p4"
  }
}
