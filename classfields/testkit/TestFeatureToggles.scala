package infra.feature_toggles.client.testkit

import common.zio.logging.Logging
import infra.feature_toggles.client.DefaultFeatureTogglesClient.Snapshot
import infra.feature_toggles.client.FeatureTogglesClient.FeatureTogglesClient
import infra.feature_toggles.client.{Decoder, Encoder, Feature, FeatureTogglesClient}
import zio._
import zio.clock.Clock

import java.util.concurrent.atomic.AtomicReference

object TestFeatureToggles {

  type TestFeatureToggles = Has[Service]

  trait Service {
    def set[T: Encoder](key: String, value: T): Unit
    final def set[T: Encoder](feature: Feature[T], value: T): Unit = set(feature.key, value)
  }

  class ServiceImpl(snapshot: AtomicReference[Option[Snapshot]]) extends Service {

    def set[T: Encoder](key: String, value: T): Unit =
      snapshot
        .updateAndGet(
          _.map(s => s.copy(toggles = s.toggles.updated(key, Encoder[T].encode(value)), version = s.version + 1))
        ): Unit
  }

  val live: ZLayer[Clock with Logging.Logging, Nothing, FeatureTogglesClient with TestFeatureToggles] = {
    val services = {
      ZIO.effectTotal(new AtomicReference[Option[Snapshot]](Some(Snapshot(toggles = Map.empty, version = 0)))).flatMap {
        storage =>
          ZIO.service[Logging.Service].map { log =>
            val service = new ServiceImpl(storage)
            val client = new FeatureTogglesClient.Service {
              override def get[T: Decoder](feature: Feature[T]): T =
                storage.get.get.toggles.get(feature.key) match {
                  case Some(value) =>
                    Decoder[T]
                      .decode(value)
                      .getOrElse(
                        throw new RuntimeException(s"Can't decode value `$value` for feature `$service/${feature.key}`")
                      )
                  case None =>
                    log.sync.debug(s"Feature `$service/${feature.key}` doesn't exist, returning default")
                    feature.default
                }
            }
            Has.allOf[FeatureTogglesClient.Service, Service](client, service)
          }
      }
    }

    services.toLayerMany
  }
}
