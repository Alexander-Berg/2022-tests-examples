package common.zio.features.testkit

import common.zio.features.Features.Features
import common.zio.features.testkit.InMemoryFeatureRegistry
import common.zio.features.{FeatureRegistryWrapper, Features}
import zio.{Ref, UIO, ULayer}

object FeaturesTest {

  val test: ULayer[Features] =
    Ref
      .make(FeatureRegistryWrapper(new InMemoryFeatureRegistry()))
      .map { registryRef =>
        new Features.Service {
          override def featureRegistry: UIO[FeatureRegistryWrapper] = registryRef.get
        }
      }
      .toLayer
}
