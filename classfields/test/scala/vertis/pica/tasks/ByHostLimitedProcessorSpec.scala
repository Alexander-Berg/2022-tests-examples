package vertis.pica.tasks

import java.time.Instant
import java.util.concurrent.TimeUnit

import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.pica.Gens.{asProducer, urlGen, QueuedImageRecordGen}
import vertis.pica.PicaControllerNamespaceComponents.Limiters
import vertis.pica.conf.ThrottlingConfig
import vertis.pica.model.{ImageRecord, Namespaces, ProcessingStatuses}
import vertis.zio.ratelimiter.{RateLimiter, RateLimiterConfig}
import vertis.zio.test.ZioSpecBase
import zio.duration.Duration
import zio.{RefM, UIO, ZIO}

import scala.concurrent.duration.DurationInt

/** @author ruslansd
  */
class ByHostLimitedProcessorSpec extends ZioSpecBase with TestOperationalSupport {

  private def processor(config: ThrottlingConfig = defaultConfig) =
    for {
      used <- RefM.make(Map.empty[String, (Instant, RateLimiter)])
      limiter <- Limiters.make(config, Namespaces.OYandex)
    } yield {
      new StaticImageProcessor(successRecord) with ByHostLimitedProcessor {
        override def usedLimitersRef: RefM[Map[String, (Instant, RateLimiter)]] = used

        override def limiters: Limiters = limiter

        override protected def qualifier: String = "main"

        override protected def prometheusRegistry: PrometheusRegistry =
          ByHostLimitedProcessorSpec.this.prometheusRegistry
      }
    }

  private def successRecord(record: ImageRecord): UIO[ImageRecord] =
    UIO(record.copy(status = ProcessingStatuses.Processed))

  private val defaultConfig: ThrottlingConfig = ThrottlingConfig(RateLimiterConfig(1000), RateLimiterConfig(1))

  "ByHostLimitedProcessor" should {

    "do not throttle different host" in ioTest {
      val queuedRecord = QueuedImageRecordGen.next(100)
      for {
        p <- processor()
        processedRecords <- ZIO.foreachPar(queuedRecord)(p.process)
        _ = processedRecords.foreach { r =>
          r.status shouldBe ProcessingStatuses.Processed
        }
      } yield ()
    }

    "throttle" in ioTest {
      val host = "test.ru"
      val cnt = 100
      val queuedRecord = QueuedImageRecordGen.next(cnt).map { r =>
        r.copy(originalUrl = urlGen(host).next.originalUrl)
      }
      processor()
        .flatMap { p =>
          ZIO.foreachPar(queuedRecord)(p.process)
        }
        .checkResult { result =>
          val now = Instant.now()
          result.count(_.status == ProcessingStatuses.Processed) shouldBe 1
          result.count(_.status == ProcessingStatuses.Queued) shouldBe cnt - 1
          result
            .filter(_.status == ProcessingStatuses.Queued)
            .forall { r =>
              r.checkTs.toEpochMilli > now.toEpochMilli
            } shouldBe true
        }
    }

    "not throttle downloading which satisfy throttling policy" in ioTest {
      val host = "test.ru"
      val limiterCfg = RateLimiterConfig(1.seconds, 1)
      val config = defaultConfig.copy(hosts = Map(host -> limiterCfg))
      val record1 = QueuedImageRecordGen.next.copy(originalUrl = urlGen(host).next.originalUrl)
      val record2 = QueuedImageRecordGen.next.copy(originalUrl = urlGen(host).next.originalUrl)
      for {
        p <- processor(config)
        processedRecord1 <- p.process(record1)
        _ <- ZIO.sleep(Duration(1, TimeUnit.SECONDS))
        processedRecord2 <- p.process(record2)
        _ = processedRecord1.status shouldBe ProcessingStatuses.Processed
        _ = processedRecord2.status shouldBe ProcessingStatuses.Processed
      } yield ()
    }

    "throttle downloading which not satisfy throttling policy" in ioTest {
      val host = "test.ru"
      val limiterCfg = RateLimiterConfig(2.seconds, 1)
      val config = defaultConfig.copy(hosts = Map(host -> limiterCfg))
      val record1 = QueuedImageRecordGen.next.copy(originalUrl = urlGen(host).next.originalUrl)
      val record2 = QueuedImageRecordGen.next.copy(originalUrl = urlGen(host).next.originalUrl)
      for {
        p <- processor(config)
        processedRecord1 <- p.process(record1)
        _ <- ZIO.sleep(Duration(1, TimeUnit.SECONDS))
        processedRecord2 <- p.process(record2)
        _ = processedRecord1.status shouldBe ProcessingStatuses.Processed
        _ = processedRecord2.status shouldBe ProcessingStatuses.Queued
      } yield ()
    }
  }
}
