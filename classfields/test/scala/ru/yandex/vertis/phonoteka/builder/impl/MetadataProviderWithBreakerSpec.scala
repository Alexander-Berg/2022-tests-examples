package ru.yandex.vertis.phonoteka.builder.impl

import java.io.IOException

import cats.effect.{ConcurrentEffect, IO, Timer}
import com.ccadllc.cedi.circuitbreaker.CircuitBreaker
import ru.yandex.vertis.phonoteka.builder.MetadataProvider
import ru.yandex.vertis.phonoteka.model.Arbitraries._
import ru.yandex.vertis.phonoteka.model.Phone
import ru.yandex.vertis.phonoteka.model.metadata.OfMetadata
import ru.yandex.vertis.phonoteka.util.CircuitBreakerSupport
import ru.yandex.vertis.quality.cats_utils.Awaitable
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.test_utils.SpecBase

import scala.concurrent.duration._
import scala.util.Try

class MetadataProviderWithBreakerSpec extends SpecBase {

  private val awaitable: Awaitable[F] = implicitly

  private val phone: Phone = generate[Phone]()

  private class SomeMetadataProvider extends MetadataProvider[F, OfMetadata] {
    override def get(phone: Phone): F[OfMetadata] =
      IO {
        throw new IOException
      }
  }

  private val provider: MetadataProvider[F, OfMetadata] =
    new SomeMetadataProvider with MetadataProviderWithBreaker[F, OfMetadata] {
      override protected val circuitBreakerConfig: CircuitBreakerSupport.Config =
        CircuitBreakerSupport.Config(sampleWindow = 1.second, degradationThreshold = 1.0, testInterval = 1.minute)
      override protected def component: String = "SomeMetadataProvider"

      implicit override protected def awaitable: Awaitable[F] = MetadataProviderWithBreakerSpec.this.awaitable
      implicit override protected def concurrentEffect: ConcurrentEffect[F] =
        MetadataProviderWithBreakerSpec.this.concurrentEffect
      implicit override protected def timer: Timer[F] = MetadataProviderWithBreakerSpec.this.timer
    }

  "MetadataProviderWithBreaker" should {
    "get" in {
      (0 to 15).foreach { _ =>
        Try(provider.get(phone).await)
        Thread.sleep(100L)
      }
      an[CircuitBreaker.OpenException] should be thrownBy provider.get(phone).await
    }
  }
}
