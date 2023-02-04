package ru.yandex.auto.searcher.catboost

import ai.catboost.CatBoostModel
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsObject, Json}
import ru.yandex.auto.catboost.{CatboostWithFeatures, CategoricalModelFeatures, ModelFeatures, NumericModelFeatures}

import scala.collection.JavaConverters._
import play.api.libs.json.Format.GenericFormat

@RunWith(classOf[JUnitRunner])
class RealtimeCatboostServiceSpec extends WordSpec {

  def testHeader(modelName: String): String =
    "==========================================" +
      s"modelName: $modelName" +
      "=========================================="

  val models = Seq(
    "autoru-online-ranking-model-1K-v3",
    "autoru-online-ranking-model-yr-1K-v3",
    // experimental models
    "model-2021-29_09-online-yr-pairwise",
    "model-2021-12-12-add-query-doc-price",
    "model-newranker-2021-12-12",
    "model-2022-01-23-add-techparams",
    "model-auction-used-2022-03-14"
  )
  "run smoke tests" in {
    models.foreach(check)
  }

  protected def check(modelName: String): Unit = {
    println(testHeader(modelName))
    val sb = new StringBuffer
    val catboostWithFeatures = CatboostWithFeatures.fromResources(modelName)
    val model: CatBoostModel = catboostWithFeatures.model
//    sb.append(model.getFeatureNames.mkString("\n"))
    sb.append(
      model.getFeatures.asScala
        .map(
          f => s"${f.getFeatureIndex}:  ${f.getName} \t ${if (!f.isUsedInModel) "— !NOT USED IN MODEL!" else ""}"
        )
        .mkString("\n")
    )

//    sb.append(catboostWithFeatures.floats.mkString("\n"))
//    sb.append(catboostWithFeatures.strings.mkString("\n"))
    sb.append("\n")
    sb.append(model.getTreeCount)
    sb.append("\n")

    sb.append(catboostWithFeatures.meta)
    sb.append("\n")

    sb.append("==========================================")

    val dummy = ModelFeatures.dummyRealtime
    catboostWithFeatures.predict(dummy)

    println(sb)
  }
}
