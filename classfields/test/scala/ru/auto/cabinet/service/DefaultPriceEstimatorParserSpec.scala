package ru.auto.cabinet.service

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.{AnyFlatSpecLike => FlatSpecLike}
import ru.auto.cabinet.model.PriceEstimation

import scala.collection.immutable.Map

class DefaultPriceEstimatorParserSpec extends FlatSpecLike with Matchers {

  behavior.of("DefaultPriceEstimatorParser")

  private val expectedMin = 215000.0d
  private val expectedMax = 970000.0d
  private val expectedAvg = 394170.0d

  private val expectedAgesToPrices = Map(
    0 -> 1035850d,
    1 -> 811320d,
    2 -> 682754d,
    3 -> 583215d,
    4 -> 523792d,
    5 -> 494721d,
    6 -> 428759d
  )

  it should "parse complete json" in {
    DefaultPriceEstimatorParser(readFile("complete.json")) should matchPattern {
      case Some(
            PriceEstimation(
              `expectedMin`,
              `expectedMax`,
              `expectedAvg`,
              `expectedAgesToPrices`)) =>
    }
  }

  it should "parse empty response json" in {
    DefaultPriceEstimatorParser(
      readFile("empty_response.json")) should matchPattern { case None =>
    }
  }

  it should "parse empty json" in {
    DefaultPriceEstimatorParser(
      readFile("empty_file.json")) should matchPattern { case None =>
    }
  }

  it should "parse json with empty age to prices array" in {
    val result =
      DefaultPriceEstimatorParser(readFile("empty_age_to_prices.json"))
    val emptyMap = Map[Int, Double]()
    result should matchPattern {
      case Some(
            PriceEstimation(
              `expectedMin`,
              `expectedMax`,
              `expectedAvg`,
              `emptyMap`)) =>
    }
  }

  it should "parse json with no age to prices field" in {
    val result =
      DefaultPriceEstimatorParser(readFile("no_age_to_prices_field.json"))
    val emptyMap = Map[Int, Double]()
    result should matchPattern {
      case Some(
            PriceEstimation(
              `expectedMin`,
              `expectedMax`,
              `expectedAvg`,
              `emptyMap`)) =>
    }
  }

  it should "parse json with malformed age to prices field" in {
    val result =
      DefaultPriceEstimatorParser(readFile("malformed_age_to_prices.json"))
    val expectedMap = Map[Int, Double](0 -> 1035850d, 3 -> 583215d)
    result should matchPattern {
      case Some(
            PriceEstimation(
              `expectedMin`,
              `expectedMax`,
              `expectedAvg`,
              `expectedMap`)) =>
    }
  }

  private def readFile(fileName: String) = {
    val file = this.getClass.getClassLoader
      .getResourceAsStream(s"price_estimator/$fileName")
    val bytes = new Array[Byte](file.available())
    file.read(bytes)
    file.close()
    new String(bytes)
  }
}
