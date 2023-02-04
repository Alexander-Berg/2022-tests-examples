package ru.yandex.auto.searcher.http.action.unit

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.Fixtures
import ru.yandex.auto.searcher.http.action.api.SearcherFeaturesHandler
import ru.yandex.auto.searcher.utils.CommonFixtures

class SearcherFeaturesHandlerSpec extends WordSpecLike with Matchers with Fixtures with CommonFixtures {

  val featuresHandler = new SearcherFeaturesHandler()(featureManager)

  "should work " in {
    print(featuresHandler.modifyFeature(featureManager.PessimizationCoefficient.name, Right("0.8")))
    print(featuresHandler.modifyFeature(featureManager.TopShuffleLimit.name, Right("10")))
    print(featuresHandler.modifyFeature(featureManager.FallbackLegacySort.name, Left("no param")))

    print(featuresHandler.modifyFeature(featureManager.FallbackLegacySort.name, Right("true")))
  }

}
