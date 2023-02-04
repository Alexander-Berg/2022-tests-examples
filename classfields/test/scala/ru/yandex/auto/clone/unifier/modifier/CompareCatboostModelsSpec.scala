package ru.yandex.auto.clone.unifier.modifier

import common.CatboostFixture
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.scalacheck.Checkers.{check, _}
import ru.yandex.auto.catboost._
import ru.yandex.auto.log.Logging
import ru.yandex.common.CatboostOfflineService

@RunWith(classOf[JUnitRunner])
class CompareCatboostModelsSpec extends WordSpecLike with Matchers with Logging with CatboostFixture {

  private val features: Gen[Stream[ModelFeatures]] = Gen.infiniteStream(genFeatures())

  private val catboostOldModel: CatboostWithFeatures =
    CatboostWithFeatures.fromResources(CatboostOfflineService.catboostOldModel)
  private val catboostNewModel: CatboostWithFeatures =
    CatboostWithFeatures.fromResources(CatboostOfflineService.catboostNewModel)
  private val catboostNewModelBy_2021_10_25: CatboostWithFeatures =
    CatboostWithFeatures.fromResources(CatboostOfflineService.catboostNewModelBy_2021_10_25)

  "make sure that models are distinct" in {
    check {
      forAll(genFeatures()) {
        case (mf) =>
          catboostOldModel.predict(mf) != catboostNewModel.predict(mf) &&
            catboostNewModel.predict(mf) != catboostNewModelBy_2021_10_25.predict(mf) &&
            catboostNewModelBy_2021_10_25.predict(mf) != catboostOldModel.predict(mf)
      }
    }
  }

  "simple display test" in {
    features.sample.get
      .take(3)
      .foreach(mf => {
        println("%s: %s".format(CatboostOfflineService.catboostOldModel, catboostOldModel.predict(mf)))
        println("%s: %s".format(CatboostOfflineService.catboostNewModel, catboostNewModel.predict(mf)))
        println(
          "%s: %s"
            .format(CatboostOfflineService.catboostNewModelBy_2021_10_25, catboostNewModelBy_2021_10_25.predict(mf))
        )
      })
  }

}
