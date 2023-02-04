package ru.yandex.vertis.moderation.price

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.extdatacore.model.verba.price.AutoruPredictPriceExclusion
import ru.yandex.vertis.moderation.model.{DetailedReason, MoneyRange}
import ru.yandex.vertis.moderation.model.autoru._
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{AutoruEssentialsGen, InstanceGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.price.PriceMismatchDecider.{Match, Mismatch, Verdict}
import ru.yandex.vertis.moderation.proto.Autoru
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.PredictPrice.{Currency => PredictPriceCurrency}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.PriceInfo.{Currency => PriceInfoCurrency}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.{
  Condition,
  CustomHouseState,
  SellerType,
  SteeringWheel
}
import ru.yandex.vertis.moderation.proto.Model.{Service, Visibility}

/**
  * [[PriceMismatchDecider]] test
  *
  * @author slider5
  */
@RunWith(classOf[JUnitRunner])
class PriceMismatchDeciderSpec extends SpecBase {

  private val autoruPredictPriceExclusionsProvider =
    new AutoruPredictPriceExclusionsProvider {

      override def get(): Set[AutoruPredictPriceExclusion] =
        Set(
          AutoruPredictPriceExclusion(mark = "VAZ", model = None, None),
          AutoruPredictPriceExclusion(mark = "MERCEDES", model = Some("A160"), None),
          AutoruPredictPriceExclusion(mark = "MERCEDES", model = Some("E300"), Some("123"))
        )
    }

  private val priceMismatchDeciderFactory = new PriceMismatchDeciderFactory(autoruPredictPriceExclusionsProvider)

  val decider: PriceMismatchDecider = priceMismatchDeciderFactory.forService(Service.AUTORU)

  "MoneyRange" should {
    "create correct interval" in {
      MoneyRange(0, 1)
      MoneyRange(0, 0)
      MoneyRange(10000, 10001)
      MoneyRange(10000, 333333)
    }
    "fail if construct illegal range" in {
      intercept[IllegalArgumentException] {
        MoneyRange(-1, 100)
      }
      intercept[IllegalArgumentException] {
        MoneyRange(1, -3)
      }
      intercept[IllegalArgumentException] {
        MoneyRange(-998, -1118)
      }
      intercept[IllegalArgumentException] {
        MoneyRange(10, 9)
      }
    }
  }

  case class TestCase(description: String, instance: Instance, expectedResult: Option[Verdict])

  "AutoruPriceMismatchDecider" should {
    val testCases =
      Seq(
        TestCase(
          description = "excluded mark",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 114)),
              Some("VAZ"),
              Some("model1"),
              Some("1"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "excluded mark, model",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 114)),
              Some("MERCEDES"),
              Some("A160"),
              Some("1"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "excluded mark, model, supergen",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 114)),
              Some("MERCEDES"),
              Some("E300"),
              Some("123"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "wrong steeringWheel",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 100)),
              Some("BMW"),
              Some("model2"),
              Some("2"),
              Some(SteeringWheel.RIGHT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "wrong sellerType",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 100)),
              Some("BMW"),
              Some("model3"),
              Some("3"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.COMMERCIAL),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "wrong customHouseState=NOT_CLEARED",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 100)),
              Some("BMW"),
              Some("model4"),
              Some("4"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.NOT_CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "wrong customHouseState=UNKNOWN",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 100)),
              Some("BMW"),
              Some("model5"),
              Some("6"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.UNKNOWN,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "sellerType is None",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 100)),
              Some("BMW"),
              Some("model7"),
              Some("7"),
              Some(SteeringWheel.LEFT),
              None,
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "steeringWheel is None",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 100)),
              Some("BMW"),
              Some("model8"),
              Some("8"),
              None,
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "mark is None",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 100)),
              None,
              Some("model9"),
              Some("9"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.UNKNOWN,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "predictPrice is None",
          instance =
            instance(
              Some(PriceInfo(5000, PriceInfoCurrency.RUB, None, None)),
              None,
              Some("BMW"),
              Some("model10"),
              Some("10"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "price is None",
          instance =
            instance(
              None,
              Some(PredictPrice(PredictPriceCurrency.RUR, 800, 1012)),
              Some("BMW"),
              Some("model11"),
              Some("11"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "condition is NEED_REPAIR",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 800, 1012)),
              Some("BMW"),
              Some("model11"),
              Some("11"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              Some(Condition.NEED_REPAIR)
            ),
          expectedResult = noneVerdict
        ),
        TestCase(
          description = "price < predictPrice.from (LOW_PRICE)",
          instance =
            instance(
              Some(PriceInfo(10, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 101)),
              Some("BMW"),
              Some("model12"),
              Some("12"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = mismatchBanVerdict(DetailedReason.LowPrice)
        ),
        TestCase(
          description = "price < predictPrice.from (DO_NOT_EXIST)",
          instance =
            instance(
              Some(PriceInfo(65, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 100, 200)),
              Some("BMW"),
              Some("model13"),
              Some("13"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = mismatchBanVerdict(DetailedReason.DoNotExist)
        ),
        TestCase(
          description = "price < predictPrice.from, [warn LOW_PRICE] lower bound actual = 0.66expected",
          instance =
            instance(
              Some(PriceInfo(66, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 100, 200)),
              Some("BMW"),
              Some("model14"),
              Some("14"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = mismatchWarnVerdict(DetailedReason.LowPrice)
        ),
        TestCase(
          description = "price < predictPrice.from, [warn LOW_PRICE] upper bound actual = 0.85expected",
          instance =
            instance(
              Some(PriceInfo(85, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 100, 200)),
              Some("BMW"),
              Some("model15"),
              Some("15"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = mismatchWarnVerdict(DetailedReason.LowPrice)
        ),
        TestCase(
          description = "price < predictPrice.from, but actual >= 0.85expected",
          instance =
            instance(
              Some(PriceInfo(86, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 100, 101)),
              Some("BMW"),
              Some("model16"),
              Some("16"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = matchVerdict
        ),
        TestCase(
          description = "price > predictPrice.from",
          instance =
            instance(
              Some(PriceInfo(81, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 101)),
              Some("BMW"),
              Some("model17"),
              Some("17"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = matchVerdict
        ),
        TestCase(
          description = "price > predictPrice.to",
          instance =
            instance(
              Some(PriceInfo(102, PriceInfoCurrency.RUB, None, None)),
              Some(PredictPrice(PredictPriceCurrency.RUR, 80, 101)),
              Some("BMW"),
              Some("model18"),
              Some("18"),
              Some(SteeringWheel.LEFT),
              Some(SellerType.PRIVATE),
              CustomHouseState.CLEARED,
              Visibility.VISIBLE,
              None
            ),
          expectedResult = matchVerdict
        )
      )

    testCases.foreach { case TestCase(description, instance, expectedResult) =>
      s"correctly work for '$description'" in {
        val actualResult = truncate(decider(instance))
        actualResult shouldBe expectedResult
      }
    }
  }

  private def instance(priceInfo: Option[PriceInfo],
                       predictPrice: Option[PredictPrice],
                       mark: Option[Mark],
                       model: Option[Model],
                       superGen: Option[String],
                       steeringWheel: Option[SteeringWheel],
                       sellerType: Option[SellerType],
                       customHouseState: CustomHouseState,
                       visibility: Visibility,
                       condition: Option[Condition]
                      ): Instance = {
    val autoru = AutoruEssentialsGen.next
    val instance = InstanceGen.next

    instance.copy(
      essentials =
        autoru.copy(
          source = Autoru.AutoruEssentials.Source.AUTO_RU,
          priceInfo = priceInfo,
          predictPrice = predictPrice,
          mark = mark.filter(_.nonEmpty),
          model = model.filter(_.nonEmpty),
          superGen = superGen.filter(_.nonEmpty),
          steeringWheel = steeringWheel,
          sellerType = sellerType,
          customHouseState = customHouseState,
          condition = condition
        ),
      context = instance.context.copy(visibility = visibility)
    )
  }

  private def truncate(verdict: Option[Verdict]): Option[Verdict] =
    verdict match {
      case Some(mismatch: Mismatch) => Some(mismatch.copy(comment = ""))
      case other                    => other
    }

  private def noneVerdict: Option[Verdict] = None
  private def matchVerdict: Option[Verdict] = Some(Match)
  private def mismatchBanVerdict(detailedReason: DetailedReason): Option[Verdict] =
    Some(Mismatch("", detailedReason, ban = true))
  private def mismatchWarnVerdict(detailedReason: DetailedReason): Option[Verdict] =
    Some(Mismatch("", detailedReason, ban = false))
}
