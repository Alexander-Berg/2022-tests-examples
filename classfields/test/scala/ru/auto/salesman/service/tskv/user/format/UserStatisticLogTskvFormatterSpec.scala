package ru.auto.salesman.service.tskv.user.format

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, ProductStatuses}
import ru.auto.salesman.service.tskv.user.domain.MetaInfo._
import ru.auto.salesman.service.tskv.user.domain._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus
import ru.yandex.vertis.util.time.DateTimeUtil

class UserStatisticLogTskvFormatterSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators {

  "Stat logger helper" should {

    "return timestamp = product activation datetime for active product" in {
      val meta = OfferMetaInfo(OfferIdentityGen.next, offerGen().next)
      val activatedAt = DateTime.parse("2019-05-24T05:06:07.123+03:00")
      forAll(
        goodsGen(activated = activatedAt, status = ProductStatuses.Active),
        paidTransactionGen()
      ) { (goods, transaction) =>
        val logUnit = LoggableEvent.Paid(
          goods,
          transaction,
          meta,
          UserModerationStatus.getDefaultInstance
        )
        val result =
          UserStatisticLogFormat.Tskv.format(logUnit).success.value
        result("timestamp") shouldBe "2019-05-24T02:06:07.123Z"
      }
    }

    "return timestamp = current time for canceled product" in {
      val meta = OfferMetaInfo(OfferIdentityGen.next, offerGen().next)
      val activatedAt = DateTime.parse("2019-05-24T05:06:07.123+03:00")
      val beforeTest = DateTimeUtil.now()
      forAll(
        goodsGen(activated = activatedAt, status = ProductStatuses.Canceled),
        paidTransactionGen()
      ) { (goods, transaction) =>
        val logUnit = LoggableEvent.Paid(
          goods,
          transaction,
          meta,
          UserModerationStatus.getDefaultInstance
        )
        val result =
          UserStatisticLogFormat.Tskv.format(logUnit).success.value
        DateTime.parse(result("timestamp")) should be >= beforeTest
        DateTime.parse(result("timestamp")) should be <= beforeTest + 1.minute
      }
    }

    "write product name into product field for placement" in {
      val meta = OfferMetaInfo(OfferIdentityGen.next, offerGen().next)
      val product = Placement
      forAll(goodsGen(goodsProduct = product), paidTransactionGen()) {
        (goods, transaction) =>
          val logUnit = LoggableEvent.Paid(
            goods,
            transaction,
            meta,
            UserModerationStatus.getDefaultInstance
          )
          val result = UserStatisticLogFormat.Tskv.format(logUnit).success.value
          result("product") shouldBe "placement"
      }
    }

    "write single vin-history with counter into product field" in {
      val meta = OfferMetaInfo(OfferIdentityGen.next, offerGen().next)
      val product = OffersHistoryReports(1)
      forAll(subscriptionGen(productGen = product), paidTransactionGen()) {
        (subscription, transaction) =>
          val logUnit = LoggableEvent.Paid(
            subscription,
            transaction,
            meta,
            UserModerationStatus.getDefaultInstance
          )
          val result = UserStatisticLogFormat.Tskv.format(logUnit).success.value
          result("product") shouldBe "offers-history-reports-1"
      }
    }

    "write vin-history package with counter into product field" in {
      val meta = OfferMetaInfo(OfferIdentityGen.next, offerGen().next)
      val product = OffersHistoryReports(10)
      forAll(subscriptionGen(productGen = product), paidTransactionGen()) {
        (subscription, transaction) =>
          val logUnit = LoggableEvent.Paid(
            subscription,
            transaction,
            meta,
            UserModerationStatus.getDefaultInstance
          )
          val result = UserStatisticLogFormat.Tskv.format(logUnit).success.value
          result("product") shouldBe "offers-history-reports-10"
      }
    }

    "write banker transaction id and drop PaymentSystemPrefix if it exists" in {
      val meta = OfferMetaInfo(OfferIdentityGen.next, offerGen().next)
      val product = Placement
      val bankerTrnId = "test_trn"
      forAll(goodsGen(goodsProduct = product), paidTransactionGen()) {
        (goods, transaction) =>
          val testTransaction =
            transaction.copy(bankerTransactionId = s"h@$bankerTrnId")
          val logUnit = LoggableEvent.Paid(
            goods,
            testTransaction,
            meta,
            UserModerationStatus.getDefaultInstance
          )
          val result = UserStatisticLogFormat.Tskv.format(logUnit).success.value
          result("banker_transaction_id") shouldBe bankerTrnId
      }
    }

    "write banker transaction id without prefix as is" in {
      val meta = OfferMetaInfo(OfferIdentityGen.next, offerGen().next)
      val product = Placement
      val bankerTrnId = "test_trn"
      forAll(goodsGen(goodsProduct = product), paidTransactionGen()) {
        (goods, transaction) =>
          val testTransaction =
            transaction.copy(bankerTransactionId = bankerTrnId)
          val logUnit = LoggableEvent.Paid(
            goods,
            testTransaction,
            meta,
            UserModerationStatus.getDefaultInstance
          )
          val result = UserStatisticLogFormat.Tskv.format(logUnit).success.value
          result("banker_transaction_id") shouldBe bankerTrnId
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
