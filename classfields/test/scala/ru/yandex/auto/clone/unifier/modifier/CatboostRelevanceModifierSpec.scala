package ru.yandex.auto.clone.unifier.modifier

import com.typesafe.config.{Config, ConfigFactory}
import common.UnifiedInfoFixture
import org.junit.runner.RunWith
import org.scalacheck.Prop
import org.scalacheck.Prop.{forAll, AnyOperators}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.scalacheck.Checkers._
import ru.yandex.auto.app.OperationalSupport
import ru.yandex.auto.catboost._
import ru.yandex.auto.clone.unifier.modifier.TestLegacyExtractor.extractModelFeatures
import ru.yandex.auto.core.model.UnifiedCarInfo
import ru.yandex.auto.dataprovider.{CatboostModelProvider, CatboostStatsProvider}
import ru.yandex.auto.log.Logging
import ru.yandex.common.CatboostOfflineService

import java.time.Instant
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class CatboostRelevanceModifierSpec extends WordSpecLike with Matchers with Logging with UnifiedInfoFixture {

  implicit lazy val config: Config = ConfigFactory.load().resolve()
  implicit val ops = new OperationalSupport
  private val modelProvider = new ResourcesS3CatboostProvider
  private val catboostService = new CatboostOfflineService(null, modelProvider)
  private val modifier = new CatboostRelevanceModifier(catboostService)
  private val modelOld = CatboostWithFeatures.fromResources(CatboostOfflineService.catboostOldModel)
  private val modelNewBy_2021_10_25 =
    CatboostWithFeatures.fromResources(CatboostOfflineService.catboostNewModelBy_2021_10_25)

  "modifier should ignore empty fields in UnifiedCarInfo" in {
    val info = new UnifiedCarInfo("")
    info.setSteeringWheel("LEFT")
    info.setColorCode("ff0000")
    info.setAutoruBodytype("sedan")

    info.getCatboostRelevance shouldEqual (0)
    modifier.modify(info)

    info.getCatboostRelevance shouldBe >(0d) // убеждаемся что пропустили результат через сигмоиду
  }

  "modifier should provide strictly capital case categories to model" in {
    val info = new UnifiedCarInfo("")
    info.setAutoruBodytype("sedan")

    info.getCatboostRelevance shouldEqual (0)
    modifier.modify(info)

    val model = CatboostWithFeatures.fromResources(CatboostOfflineService.catboostOldModel)

    info.getCatboostRelevance shouldEqual model
      .predict(
        ModelFeatures.dummy
          .copy(categorical = {
            val dummy = CategoricalModelFeatures.dummy
            dummy.array.update(2, "SEDAN")
            dummy
          })
      )
  }

  "modifier should fill in experimental model relevance" in {
    val info = new UnifiedCarInfo("")
    info.setAutoruBodytype("sedan")

    info.getCatboostRelevance shouldEqual (0)
    modifier.modify(info)

    info.getCatboostRelevance shouldBe >(0d)
    info.getCatboostRelevanceBoosted shouldBe >(0d)
    info.getOfflineCatboostModelRelevance.length shouldEqual (2)

    println(info.getOfflineCatboostModelRelevance.mkString(","))

    info.getOfflineCatboostModelRelevance.iterator
      .find(_.getName == CatboostOfflineService.catboostOldModel)
      .map(r => (r.getRelevance, r.getBoostedRelevance))
      .exists(_._1 == info.getCatboostRelevance) shouldBe true

    info.getCatboostRelevance shouldEqual modelOld
      .predict(
        ModelFeatures.dummy
          .copy(categorical = {
            val dummy = CategoricalModelFeatures.dummy
            dummy.array.update(2, "SEDAN")
            dummy
          })
      )
  }

  private def compare(info: UnifiedCarInfo, modelName: String, model: CatboostWithFeatures): Prop = {
    val mf = extractModelFeatures(info)

    modifier.modify(info)
    def getModel(model: String): Double = {
      val modifiedValue = info.getOfflineCatboostModelRelevance
        .find(_.getName == model)
        .map(
          _.getRelevance
        )
        .get
      modifiedValue
    }

    getModel(modelName) ?= model.predict(mf)
  }

  "check if feature order is correct with OLD model" in {
    check {
      forAll(genUnifiedInfo()) { info =>
        compare(info, CatboostOfflineService.catboostOldModel, modelOld)
      }
    }
  }

  "check if feature order is correct with NEW model (2021-10-25)" in {
    check {
      forAll(genUnifiedInfo()) { info =>
        compare(info, CatboostOfflineService.catboostNewModelBy_2021_10_25, modelNewBy_2021_10_25)
      }
    }
  }

}

class TestCatboostStatsProvider extends CatboostStatsProvider(null, null) {
  override def update(): Unit = {}
}

class ResourcesS3CatboostProvider(implicit val cfg: Config) extends CatboostModelProvider(null, null) {
  override def getLatest: CatboostWithFeatures = CatboostWithFeatures.fromResources()

  override def update(): Unit = {}
}

/**
  * Временный миграционный тест-кейс, после добавления новых фичей можно TestLegacyExtractor можно удалить
  */
object TestLegacyExtractor {

  val statsDummy: Array[Float] = {
    Array.fill(CatboostModelSize.statsFeaturesSize)(0f)
  }

  def extractModelFeatures(info: UnifiedCarInfo) = {
    ModelFeatures(extractInfoFeatures(info), extractFeatures(info, info.getGeobaseId))
  }

  private def extractInfoFeatures(
      info: UnifiedCarInfo
  ): NumericModelFeatures = {

    val numericStatisticsPack: Option[Array[Float]] = info.maybeNumericStatisticsPack
    val infoProperties = info.getCatboostNumericalFeatures

    NumericModelFeatures(
      infoProperties ++ numericStatisticsPack.getOrElse(statsDummy)
    )
  }

  private def extractFeatures(info: UnifiedCarInfo, federationId: String): CategoricalModelFeatures = {
    CategoricalModelFeatures(info.getCatboostCategoryFeatures(federationId))
  }

  private val uppercaseOrDefault: String => String = s => Try(s.toUpperCase).getOrElse("")
  implicit class TestRichUnifiedCarInfo(val info: UnifiedCarInfo) extends AnyVal {

    def getCatboostCategoryFeatures(federationId: String): Array[String] =
      Array(
        info.getGearType,
        info.getColorFull,
        info.getAutoruBodytype,
        info.getEngineTypeFull,
        Try(info.getOwnersCount.toString).getOrElse(""),
        federationId,
        info.getTransmissionFull,
        info.getSegment,
        info.getAutoClass,
        info.getMark,
        info.getModel,
        Try(info.getConfigurationId.toString).getOrElse(""),
        Try(info.getTechParamId.toString).getOrElse(""),
        info.getSection,
        Try(info.getSourceInfo.getSource).getOrElse("")
      ).map(uppercaseOrDefault)

    def getCatboostNumericalFeatures: Array[Float] = {
      val nowSeconds = Instant.now.getEpochSecond
      val ageCreated = Try(nowSeconds.toFloat - (info.getCreationDate / 1000)).getOrElse(0f)
      val ageUpdated = Try(nowSeconds.toFloat - (info.getUpdateDate / 1000)).getOrElse(0f)

      Array(
        Try(info.getPrice.toFloat).getOrElse(0f),
        Try(info.getMileage.toFloat).getOrElse(0f),
        Try(info.getYear.toFloat).getOrElse(0f),
        Try(info.getHorsePower.toFloat).getOrElse(0f),
        Try(info.getDisplacement.toFloat).getOrElse(0f),
        ageCreated,
        ageUpdated,
        Try(info.getPredictPrice.getMean).getOrElse(0f),
        Try(info.getPredictPrice.getMean / info.getPrice).getOrElse(0f)
      )
    }

    def maybeNumericStatisticsPack: Option[Array[Float]] = {
      Option(info.getNumericFeaturePairs).map(m => m.map(_.getValue))
    }

  }
}
