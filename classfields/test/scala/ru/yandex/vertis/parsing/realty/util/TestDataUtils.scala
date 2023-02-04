package ru.yandex.vertis.parsing.realty.util

import java.util.concurrent.atomic.AtomicInteger

import org.joda.time.DateTime
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.realty.ParsingRealtyModel.{OfferCategory, OfferType, ParsedOffer}
import ru.yandex.vertis.parsing.realty.dao.offers.ParsedRealtyRow
import ru.yandex.vertis.parsing.realty.parsers.CommonRealtyParser
import ru.yandex.vertis.parsing.util.EnumUtils
import ru.yandex.vertis.parsing.util.RandomUtil.{choose, randomSymbols}

/**
  * Created by andrey on 2/14/18.
  */
object TestDataUtils {
  private val testId: AtomicInteger = new AtomicInteger(60873378)

  // -------------------------------------------

  def testAvitoApartmentUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    "https://www.avito.ru/lyskovo/kvartiry/1-k_kvartira_31_m_25_et._" + offerId
  }

  def testCianApartmentUrl: String = {
    val offerId: Int = testId.getAndIncrement()
    "https://vidnoye.cian.ru/sale/flat/" + offerId + "/"
  }

  //scalastyle:off method.length
  //scalastyle:off parameter.number
  def testRow(url: String,
              parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder(),
              offerType: OfferType = OfferType.SELL,
              offerCategory: OfferCategory = OfferCategory.APARTMENT,
              geobaseId: Long = 0,
              phone: Option[String] = None,
              callCenter: Option[String] = None,
              source: CommonModel.Source = CommonModel.Source.SCRAPING_HUB_FRESH,
              deactivateDate: Option[DateTime] = None,
              updated: DateTime = DateTime.now()): ParsedRealtyRow = {
    val site = Site.fromUrl(url)
    val remoteId = CommonRealtyParser.remoteId(url)
    val hash = CommonRealtyParser.hash(url)
    if (EnumUtils.notValidEnumValue(parsedOffer.getOffer.getOfferType)) {
      parsedOffer.getOfferBuilder.setOfferType(offerType)
    }
    if (EnumUtils.notValidEnumValue(parsedOffer.getOffer.getOfferCategory)) {
      parsedOffer.getOfferBuilder.setOfferCategory(offerCategory)
    }
    if (parsedOffer.getOffer.getRemoteId.isEmpty) {
      parsedOffer.getOfferBuilder.setRemoteId(remoteId)
    }
    if (parsedOffer.getOffer.getRemoteUrl.isEmpty) {
      parsedOffer.getOfferBuilder.setRemoteUrl(url)
    }
    if (parsedOffer.getHash.isEmpty) parsedOffer.setHash(hash)
    if (geobaseId != 0) {
      parsedOffer.getOfferBuilder.getLocationBuilder.setGeobaseId(geobaseId)
    }
    if (phone.nonEmpty) {
      parsedOffer.getOfferBuilder.getSellerBuilder.addPhone(phone.get)
    }
    ParsedRealtyRow(
      id = 0,
      hash = hash,
      offerType = offerType,
      offerCategory = offerCategory,
      status = CommonModel.Status.NEW,
      site = site,
      url = url,
      data = parsedOffer.build(),
      createDate = updated,
      updateDate = updated,
      statusUpdateDate = updated,
      sentDate = None,
      openDate = None,
      deactivateDate = deactivateDate,
      source = source,
      callCenter = callCenter,
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
}
