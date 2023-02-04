package ru.yandex.auto.catboost

import ai.catboost.CatBoostModel
import org.scalatest.WordSpec

class CatboostSpec extends WordSpec {
  private val catboostWithFeatures: CatboostWithFeatures = CatboostWithFeatures.fromResources()
  val model: CatBoostModel = catboostWithFeatures.model

  "smoke test" in {
    println("=== CatBoost model stats ===")
    println("getFeatureNames " + model.getFeatureNames.toList)
    println("getPredictionDimension " + model.getPredictionDimension)
    println("getUsedCategoricFeatureCount " + model.getUsedCategoricFeatureCount)
    println("getUsedNumericFeatureCount " + model.getUsedNumericFeatureCount)

    val prediction = catboostWithFeatures.predict(ModelFeatures.dummy)
    println("model value is " + prediction)
  }

  "pass categories test" in {
    // fill from https://autocenter.vertis.yandex-team.ru/api/offers/1097214912-1b494803/shard?category=CARS
    val cats = Array(
      "LEFT",
      "ff0000",
      "SEDAN",
      "GASOLINE",
      "1",
      "C_FEDERAL_SUBJECT_ID?",
      "AUTOMATIC",
      "C_SEGMENT?",
      "C_AUTO_CLASS?",
      "MITSUBISHI",
      "LANCER",
      "C_CONFIGURATION?",
      "C_TECH_PARAM?",
      "C_SECTION?",
      "desktop"
    )
    val numeric = Array.fill(44)(0f)

    val prediction = catboostWithFeatures.predictions(ModelFeatures.from(numeric, cats))
    println("getObjectCount " + prediction.getObjectCount)
    println("getPredictionDimension " + prediction.getPredictionDimension)
    println("model value is " + prediction.get(0, 0))
  }
}
