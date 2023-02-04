package ru.yandex.vertis.zio_baker.deduplicator

import com.softwaremill.tagging._
import common.ops.prometheus.DefaultMetricsSupport
import common.zio.ops.prometheus.Prometheus
import common.zio.ops.prometheus.Prometheus.Prometheus
import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.prometheus.{PrometheusRegistry, SimpleCompositeCollector}
import ru.yandex.vertis.zio_baker.cached.{Cache, Layouts}
import ru.yandex.vertis.zio_baker.model.Tag.ServerName
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{Has, UIO, ULayer, URLayer, ZIO, ZLayer}

object KeyDeduplicatorSpec extends DefaultRunnableSpec {

  protected lazy val prometheusLayer: ULayer[Prometheus] =
    ZLayer.succeed(new Prometheus.Service {

      override def registry: UIO[PrometheusRegistry] = UIO.succeed(new SimpleCompositeCollector())

      override def metricsSupport: UIO[MetricsSupport] = registry.map(DefaultMetricsSupport.apply)
    })

  private lazy val deduplicatorLayer: URLayer[Clock, KeyDeduplicator] =
    prometheusLayer ++
      ZLayer.requires[Clock] ++
      ZLayer.succeed("serverName".taggedWith[ServerName]) ++
      cacheBackend >>> KeyDeduplicator.live

  private lazy val cacheBackend: ULayer[Has[Cache.Service[String, String]]] = Cache.async(
    new InMemoryAsyncCache[String, String](Layouts.StringLayout)
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val idempotencyKey1 = "some-key"
    val idempotencyKey2 = "some-key2"
    val someValue1 = "value"
    val someValue2 = "value2"
    suite("KeyDeduplicator")(
      testM("deduplicates by idempotency key") {
        val res = for {
          deduplicator <- ZIO.service[KeyDeduplicator.Service]
          response1 <- deduplicator.tryApply(idempotencyKey1, "action")(ZIO.some(someValue1))(ZIO.none)
          response2 <- deduplicator.tryApply(idempotencyKey1, "action")(ZIO.some(someValue2))(ZIO.none)
        } yield (response1, response2)
        assertM(res)(equalTo((Some(someValue1), None))).provideLayer(deduplicatorLayer)
      },
      testM("does not deduplicate for different idempotency keys") {
        val res = for {
          deduplicator <- ZIO.service[KeyDeduplicator.Service]
          response1 <- deduplicator.tryApply(idempotencyKey1, "action")(ZIO.some(someValue1))(ZIO.none)
          response2 <- deduplicator.tryApply(idempotencyKey2, "action")(ZIO.some(someValue2))(ZIO.none)
        } yield (response1, response2)
        assertM(res)(equalTo((Some(someValue1), Some(someValue2)))).provideLayer(deduplicatorLayer)
      }
    )
  }
}
