package common

import org.scalacheck.Gen
import ru.yandex.auto.catboost.{CatboostModelSize, CategoricalModelFeatures, ModelFeatures, NumericModelFeatures}
import ru.yandex.auto.core.model.ShortCarAd

trait CatboostFixture {

  def genFeatures(): Gen[ModelFeatures] =
    for {
      floats <- Gen.infiniteStream(Gen.choose(0f, 1f))
      strings <- Gen.infiniteStream(Gen.choose(0f, 1f).map(_.toString))
    } yield {
      ModelFeatures(
        NumericModelFeatures(floats.take(CatboostModelSize.numFeaturesSize).toArray),
        CategoricalModelFeatures(strings.take(CatboostModelSize.catFeaturesSize).toArray)
      )
    }
}
