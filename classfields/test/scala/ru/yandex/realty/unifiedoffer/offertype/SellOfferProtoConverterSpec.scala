package ru.yandex.realty.unifiedoffer.offertype

import org.joda.time.{Duration, Instant}
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.model.history.{PriceHistory => InternalPriceHistory}
import ru.yandex.realty.model.offer._
import ru.yandex.realty.proto.offer.{DealStatus => ProtoDealStatus, DealType => ProtoDealType}
import ru.yandex.realty.unifiedoffer.help.ConvertSettings
import ru.yandex.realty.{SpecBase, StorageBuilder}

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class SellOfferProtoConverterSpec extends SpecBase with Matchers {

  /*
  val converter = new SellOfferProtoConverter(StorageBuilder.buildCurrencyStorage())
  val convertSettings = ConvertSettings(Currency.RUR, AreaUnit.SQUARE_METER)

  val now = new Instant
  val TWO_DAYS: Duration = Duration.standardDays(2)

  "SellOfferProtoConverter.toMessage " should {
    "Convert full fields with history " in {
      val offer = buildOffer()
      val history = InternalPriceHistory.EMPTY
        .append(now.minus(TWO_DAYS), Money.scaledOf(Currency.RUR, 2000000000L))

      val result = converter.toMessage(offer, convertSettings, Some(history))

      result.hasPrice should be(true)
      result.getPrice.hasPrice should be(true)
      result.getPrice.hasHistory should be(true)
      result.hasMortgageInfo should be(true)
      result.hasTransactionCondition should be(true)
      result.hasPredictedPrice should be(true)
      result.getDealStatus should be(ProtoDealStatus.DEAL_STATUS_LAW214)
      result.getDealType should be(ProtoDealType.DEAL_TYPE_ALTERNATIVE)
    }

    "Convert without history " in {
      val offer = buildOffer()
      val result = converter.toMessage(offer, convertSettings)

      result.hasPrice should be(true)
      result.getPrice.hasPrice should be(true)
      result.getPrice.hasHistory should be(false)
      result.hasMortgageInfo should be(true)
      result.hasTransactionCondition should be(true)
      result.hasPredictedPrice should be(true)
      result.getDealStatus should be(ProtoDealStatus.DEAL_STATUS_LAW214)
      result.getDealType should be(ProtoDealType.DEAL_TYPE_ALTERNATIVE)
    }
  }

  def buildOffer(): Offer = {
    val area = AreaInfo.create(AreaUnit.SQUARE_METER, 50.0f)
    val priceInfo = PriceInfo.create(Currency.RUR, 17000000000.0f, PricingPeriod.WHOLE_LIFE, AreaUnit.WHOLE_OFFER)
    val transaction = new Transaction
    transaction.setAreaPrice(new AreaPrice(priceInfo, area))
    transaction.setDealType(DealType.ALTERNATIVE)
    transaction.setDealStatus(DealStatus.LAW214)
    transaction.setTransactionCondition(TransactionCondition.CLEANING_INCLUDED, true)

    val pricePrediction = new PricePrediction(Currency.RUR, 1.0f, 3.0f, 2.0f)
    val predictions = new Predictions()
    predictions.setPricePrediction(pricePrediction)

    val offer = new Offer()
    offer.setArea(area)
    offer.setTransaction(transaction)
    offer.setPrimarySaleV2(true)
    offer.setMortgageApprove(10)
    offer.setPredictions(predictions)

    offer
  }
 */

}
