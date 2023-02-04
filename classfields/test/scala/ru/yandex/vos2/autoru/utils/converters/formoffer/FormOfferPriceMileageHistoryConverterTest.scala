package ru.yandex.vos2.autoru.utils.converters.formoffer

import com.google.protobuf.util.Timestamps
import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.CommonModel.PriceInfo
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Mileage, Price}
import ru.yandex.vos2.BasicsModel.Currency
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.utils.FormTestUtils
import ru.yandex.vos2.autoru.utils.booking.impl.EmptyDefaultBookingAllowedDeciderImpl

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class FormOfferPriceMileageHistoryConverterTest extends AnyWordSpec with InitTestDbs {
  initDbs()

  private val formTestUtils = new FormTestUtils(components)
  import formTestUtils._

  private val formOfferConverter: FormOfferConverter =
    new FormOfferConverter(
      components.carsCatalog,
      components.recognizedLpUtils,
      EmptyDefaultBookingAllowedDeciderImpl,
      components.featuresManager
    )

  private val yearAgo = System.currentTimeMillis() - (60 * 60 * 1000 * 24 * 365)
  private val monthAgo = System.currentTimeMillis() - (60 * 60 * 1000 * 24 * 30)
  private val now = System.currentTimeMillis()

  final private case class TestMileageHistoryCase(description: String,
                                                  previousMileageHistory: Seq[Mileage],
                                                  formMileage: Int,
                                                  expectedMileageHistory: Seq[Mileage],
                                                  moderator: Boolean,
                                                  mileageFakeSuspicion: Boolean = false)

  final private case class TestPriceHistoryCase(description: String,
                                                previousPriceHistory: Seq[Price],
                                                formPrice: PriceInfo,
                                                expectedPriceHistory: Seq[Price],
                                                moderator: Boolean)

  private val testMileageHistoryCases: Seq[TestMileageHistoryCase] = Seq(
    TestMileageHistoryCase(
      description = "Add new mileage to history with moderator changes",
      previousMileageHistory = Seq(
        Mileage.newBuilder().setMileage(1000).setUpdateTimestamp(Timestamps.fromMillis(yearAgo)).build(),
        Mileage.newBuilder().setMileage(10000).setUpdateTimestamp(Timestamps.fromMillis(monthAgo)).build()
      ),
      formMileage = 15000,
      expectedMileageHistory = Seq(
        Mileage.newBuilder().setMileage(1000).setUpdateTimestamp(Timestamps.fromMillis(yearAgo)).build(),
        Mileage.newBuilder().setMileage(10000).setUpdateTimestamp(Timestamps.fromMillis(monthAgo)).build(),
        Mileage
          .newBuilder()
          .setMileage(15000)
          .setUpdateTimestamp(Timestamps.fromMillis(now))
          .setEditedByModerator(true)
          .setFakeSuspicionByModerator(true)
          .build()
      ),
      moderator = true,
      mileageFakeSuspicion = true
    ),
    TestMileageHistoryCase(
      description = "Add new mileage to history without moderator changes",
      previousMileageHistory = Seq(
        Mileage.newBuilder().setMileage(1000).setUpdateTimestamp(Timestamps.fromMillis(yearAgo)).build(),
        Mileage.newBuilder().setMileage(10000).setUpdateTimestamp(Timestamps.fromMillis(monthAgo)).build()
      ),
      formMileage = 15000,
      expectedMileageHistory = Seq(
        Mileage.newBuilder().setMileage(1000).setUpdateTimestamp(Timestamps.fromMillis(yearAgo)).build(),
        Mileage.newBuilder().setMileage(10000).setUpdateTimestamp(Timestamps.fromMillis(monthAgo)).build(),
        Mileage
          .newBuilder()
          .setMileage(15000)
          .setUpdateTimestamp(Timestamps.fromMillis(now))
          .build()
      ),
      moderator = false
    )
  )

  private val testPriceHistoryCases: Seq[TestPriceHistoryCase] = Seq(
    TestPriceHistoryCase(
      description = "Add new price to history with moderator changes",
      previousPriceHistory = Seq(
        Price.newBuilder().setPrice(1000).setCurrency(Currency.RUB).setCreated(yearAgo).build(),
        Price.newBuilder().setPrice(10000).setCurrency(Currency.RUB).setCreated(monthAgo).build()
      ),
      formPrice = PriceInfo.newBuilder().setPrice(150000).setCurrency("RUB").setCreateTimestamp(now).build(),
      expectedPriceHistory = Seq(
        Price.newBuilder().setPrice(1000).setCurrency(Currency.RUB).setCreated(yearAgo).build(),
        Price.newBuilder().setPrice(10000).setCurrency(Currency.RUB).setCreated(monthAgo).build(),
        Price
          .newBuilder()
          .setPrice(150000)
          .setCurrency(Currency.RUB)
          .setPriceRub(150000d)
          .setCreated(now)
          .setEditedByModerator(true)
          .build()
      ),
      moderator = true
    ),
    TestPriceHistoryCase(
      description = "Add new price to history without moderator changes",
      previousPriceHistory = Seq(
        Price.newBuilder().setPrice(1000).setCurrency(Currency.RUB).setCreated(yearAgo).build(),
        Price.newBuilder().setPrice(10000).setCurrency(Currency.RUB).setCreated(monthAgo).build()
      ),
      formPrice = PriceInfo.newBuilder().setPrice(150000).setCurrency("RUB").setCreateTimestamp(now).build(),
      expectedPriceHistory = Seq(
        Price.newBuilder().setPrice(1000).setCurrency(Currency.RUB).setCreated(yearAgo).build(),
        Price.newBuilder().setPrice(10000).setCurrency(Currency.RUB).setCreated(monthAgo).build(),
        Price
          .newBuilder()
          .setPrice(150000)
          .setCurrency(Currency.RUB)
          .setPriceRub(150000d)
          .setCreated(now)
          .build()
      ),
      moderator = false
    )
  )

  "FormOfferConverter.convertOffer" when {
    testMileageHistoryCases.foreach {
      case TestMileageHistoryCase(
          description,
          previousMileageHistory,
          formMileage,
          expectedMileageHistory,
          moderator,
          fakeSuspicion
          ) =>
        description in {
          val form = {
            val builder = privateOfferForm.toBuilder
            builder.getStateBuilder.setMileage(formMileage)
            builder.build
          }
          val curOffer = {
            val builder = curPrivateProto.toBuilder
            builder.getOfferAutoruBuilder.clearMileageHistory()
            builder.getOfferAutoruBuilder.addAllMileageHistory(previousMileageHistory.asJava)
            builder.build
          }
          val result = formOfferConverter.convertExistingOffer(
            form = form,
            curOffer = curOffer,
            optDraft = None,
            ad = privateAd,
            now = now,
            params = FormWriteParams.empty.copy(moderator = moderator, isMileageFakeSuspicion = fakeSuspicion)
          )
          val actualHistory = result.getOfferAutoru.getMileageHistoryList.asScala
          assert(actualHistory == expectedMileageHistory)
        }
    }

    testPriceHistoryCases.foreach {
      case TestPriceHistoryCase(description, previousPriceHistory, formPrice, expectedPriceHistory, moderator) =>
        description in {
          val form = {
            val builder = privateOfferForm.toBuilder
            builder.setPriceInfo(formPrice)
            builder.build
          }
          val curOffer = {
            val builder = curPrivateProto.toBuilder
            builder.getOfferAutoruBuilder.clearPriceHistory()
            builder.getOfferAutoruBuilder.addAllPriceHistory(previousPriceHistory.asJava)
            builder.build
          }
          val result = formOfferConverter.convertExistingOffer(
            form = form,
            curOffer = curOffer,
            optDraft = None,
            ad = privateAd,
            now = now,
            params = FormWriteParams.empty.copy(moderator = moderator)
          )
          val actualHistory = result.getOfferAutoru.getPriceHistoryList.asScala.toList
          assert(actualHistory == expectedPriceHistory)
        }
    }
  }
}
