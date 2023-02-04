package ru.yandex.auto.vin.decoder.manager.vin.score

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.mockito.MockitoSupport

class HealthScoreCalculatorTest extends AnyWordSpecLike with Matchers with MockitoSupport {

  private val calculator = new HealthScoreCalculator

  private val ownerScore = Map(
    0 -> 0.7f,
    1 -> 0.7f,
    2 -> .525f,
    3 -> .35f,
    4 -> .175f,
    5 -> .175f,
    6 -> .175f,
    7 -> .07f,
    10 -> .07f
  )

  private val dtpScore = Map(
    List.empty -> .5f,
    List(101, 15) -> .05f, // тотал
    List(110, 120, 130) -> 0.35f, // 1 - 0.1 * 3
    List(110, 271) -> 0.3f, // 1 - 0.3 - 0.1
    List(110, 180, 220, 271, 275) -> .05f // меньше минимума
  )

  "score" should {
    "return correct value" in {
      val res = calculator.getScore(
        year = 2012,
        mark = "AUDI",
        hasFreshCertificates = false,
        usedInCarsharing = false,
        hasLegalConstraints = false,
        wanted = false,
        lastMileage = 120000,
        usedInTaxi = false,
        dtpDamageCodes = Seq.empty,
        ownersCount = 2,
        calcScoreAtCustomYear = Some(2021)
      )

      res.score shouldBe 84.0f
    }
    "return correct score for test data set" in {
      val inputs = ResourceUtils.getLines("/score/test_input.csv")
      inputs
        .drop(1)
        .foreach(csv => {
          val model = TestScoreInput.fromCsvLine(csv)

          val res = calculator.getScore(
            model.year,
            model.mark,
            model.hasFreshBrandCerts,
            model.usedInCarsharing,
            model.hasLegalCostraints,
            model.wanted,
            model.lastMileage,
            model.usedInTaxi,
            model.accidentDamageCodes,
            model.ownersCount,
            calcScoreAtCustomYear = Some(2021)
          )

          res.score shouldBe model.res
        })

    }
  }

  "owner score" should {
    "return correct score" in {
      ownerScore.foreach { case (ownerCount, res) =>
        calculator.getOwnersScore(ownerCount) shouldBe res
      }

    }
  }

  "dtp score" should {
    "return correct score" in {
      dtpScore.foreach { case (damages, res) =>
        println(damages)
        calculator.getDtpScore(damages) shouldBe res
      }
    }
  }

}
