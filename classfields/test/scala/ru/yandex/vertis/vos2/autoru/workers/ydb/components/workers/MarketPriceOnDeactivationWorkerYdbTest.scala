package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.mockito.Mockito
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.worker.YdbShouldProcessResult
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.MarketPriceOnDeactivationWorkerYdbTest._
import ru.auto.api.StatsModel.{PredictPrice, PriceRange}
import ru.auto.api.search.SearchModel
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Market, SellerType}
import ru.yandex.vos2.BasicsModel
import ru.yandex.vos2.OfferModel.{OfferFlag, OfferStatusHistoryItem}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.pricepredict.PricePredictClient
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}

import scala.util.Success

class MarketPriceOnDeactivationWorkerYdbTest
  extends AnyWordSpec
  with InitTestDbs
  with MockitoSupport
  with Matchers
  with BeforeAndAfter {

  private val pricePredictClient = mock[PricePredictClient]

  before {
    Mockito.reset(pricePredictClient)
    when(pricePredictClient.getPredicts(?, ?)(?)).thenCallRealMethod()
    when(pricePredictClient.requiredParams(?)).thenReturn(true)
  }

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val featureRegistry = FeatureRegistryFactory.inMemory()
    val featuresManager = new FeaturesManager(featureRegistry)

    val worker = new MarketPriceOnDeactivationWorkerYdb(
      pricePredictClient
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }

  "alreadyProcessed" should {
    "return False on empty state" in new Fixture {
      val offer = TestUtils.createOffer()
      worker.alreadyProcessed(offer.build(), state = None) shouldBe false
    }

    "return False on new status change" in new Fixture {
      val offer = TestUtils.createOffer()

      offer
        .clearStatusHistory()
        .addStatusHistory {
          OfferStatusHistoryItem
            .newBuilder()
            .setTimestamp {
              DateTime.parse("2021-01-01T10:00:00.000+03:00").getMillis
            }
        }
        .addStatusHistory {
          OfferStatusHistoryItem
            .newBuilder()
            .setTimestamp {
              DateTime.parse("2021-01-01T12:00:00.000+03:00").getMillis
            }
        }

      val state = Some("2021-01-01T11:00:00.000+03:00")

      worker.alreadyProcessed(offer.build(), state) shouldBe false
    }

    "return True on already processed status change" in new Fixture {
      val offer = TestUtils.createOffer()

      offer
        .clearStatusHistory()
        .addStatusHistory {
          OfferStatusHistoryItem
            .newBuilder()
            .setTimestamp {
              DateTime.parse("2021-01-01T10:00:00.000+03:00").getMillis
            }
        }
        .addStatusHistory {
          OfferStatusHistoryItem
            .newBuilder()
            .setTimestamp {
              DateTime.parse("2021-01-01T12:00:00.000+03:00").getMillis
            }
        }

      val state = Some("2021-01-01T12:00:00.000+03:00")

      worker.alreadyProcessed(offer.build(), state) shouldBe true
    }
  }

  "shouldProcess" should {
    "return True for INACTIVE and DELETED offers" in new Fixture {
      OfferFlagsToProcess.foreach { flag =>
        val offer = TestUtils.createOffer(
          dealer = true,
          category = Category.CARS
        )

        offer
          .clearFlag()
          .addFlag(flag)

        val state = None

        val expected = YdbShouldProcessResult(shouldProcess = true)
        worker.shouldProcess(offer.build(), state) shouldBe expected
      }
    }

    "return False for other offer statuses" in new Fixture {
      val offerFlags = OfferFlag.values().filterNot(OfferFlagsToProcess.contains)

      offerFlags.foreach { flag =>
        val offer = TestUtils.createOffer(
          dealer = true,
          category = Category.CARS
        )

        offer
          .clearFlag()
          .addFlag(flag)

        val state = None

        val expected = YdbShouldProcessResult(shouldProcess = false)
        worker.shouldProcess(offer.build(), state) shouldBe expected
      }
    }

    "return False for non-dealer offers" in new Fixture {
      OfferFlagsToProcess.foreach { flag =>
        val offer = TestUtils.createOffer(
          dealer = false,
          category = Category.CARS
        )

        offer
          .clearFlag()
          .addFlag(flag)

        val state = None

        val expected = YdbShouldProcessResult(shouldProcess = false)
        worker.shouldProcess(offer.build(), state) shouldBe expected
      }
    }

    "return False for non-cars offers" in new Fixture {
      OfferFlagsToProcess.foreach { flag =>
        Seq(Category.MOTO, Category.TRUCKS).foreach { category =>
          val offer = TestUtils.createOffer(
            dealer = true,
            category = category
          )

          offer
            .clearFlag()
            .addFlag(flag)

          val state = None

          val expected = YdbShouldProcessResult(shouldProcess = false)
          worker.shouldProcess(offer.build(), state) shouldBe expected
        }
      }
    }

    "return False for offer with missing required params for predictor-api" in new Fixture {
      OfferFlagsToProcess.foreach { flag =>
        val offer = TestUtils.createOffer(
          dealer = true,
          category = Category.CARS
        )

        offer
          .clearFlag()
          .addFlag(flag)

        when(pricePredictClient.requiredParams(?)).thenReturn(false)

        val state = None

        val expected = YdbShouldProcessResult(shouldProcess = false)
        worker.shouldProcess(offer.build(), state) shouldBe expected

        Mockito.verify(pricePredictClient).requiredParams(offer.getOfferAutoru)
      }
    }
  }

  "process" should {
    "return update market price function" in new Fixture {
      val offerBuilder = TestUtils.createOffer(dealer = true)

      offerBuilder.getOfferAutoruBuilder
        .clearMarketPriceOnDeactivation()

      offerBuilder
        .clearStatusHistory()
        .addStatusHistory {
          OfferStatusHistoryItem
            .newBuilder()
            .setTimestamp {
              DateTime.parse("2021-01-01T10:00:00.000+03:00").getMillis
            }
        }

      val offer = offerBuilder.build()

      val predictedPrice = PredictPrice
        .newBuilder()
        .setAutoru {
          PriceRange
            .newBuilder()
            .setFrom(1000)
            .setTo(2000)
            .setCurrency(SearchModel.Currency.RUR)
        }
        .build()

      when(pricePredictClient.predict(?, ?)(?)).thenReturn(Success(Some(predictedPrice)))

      val result = worker.process(offer, state = None)

      result.updateOfferFunc.nonEmpty shouldBe true
      result.nextCheck shouldBe empty
      result.nextState shouldBe Some("2021-01-01T10:00:00.000+03:00")

      val modifiedOffer = result.updateOfferFunc.get(offer)

      val expectedMarketPriceOnDeactivation = Market
        .newBuilder()
        .setPrice(1500)
        .setCurrency(BasicsModel.Currency.RUB)
        .build()

      modifiedOffer.getOfferAutoru.getMarketPriceOnDeactivation shouldBe expectedMarketPriceOnDeactivation

      Mockito.verify(pricePredictClient).getPredicts(offer, SellerType.COMMERCIAL)
    }
  }

}

object MarketPriceOnDeactivationWorkerYdbTest {
  val OfferFlagsToProcess = Seq(OfferFlag.OF_INACTIVE, OfferFlag.OF_DELETED)
}
