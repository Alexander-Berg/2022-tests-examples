package ru.yandex.auto.extdata.jobs.feeds.feed.writers

import io.prometheus.client.Counter
import org.junit.runner.RunWith
import org.mockito.Mockito.{doNothing, verify}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.extdata.jobs.feeds.feed.utils.FeedPropertiesTest
import ru.yandex.auto.extdata.jobs.feeds.feed.writers.FeedWriter.FeedEntry
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry

@RunWith(classOf[JUnitRunner])
class FeedWriterMeteredProxySpec extends WordSpec with Matchers {
  import FeedWriterMeteredProxySpec.TestEnv

  "FeedWriterMeteredProxy.start" should {
    "delegate the call" in new TestEnv[Unit] {
      proxyToTest.start
      verify(writerMock).start
    }
  }

  "FeedWriterMeteredProxy.write" should {
    "delegate the call" in new TestEnv[Unit] {
      proxyToTest.write(FeedEntry((), Seq()))
      verify(writerMock).write(?)
    }
  }

  "FeedWriterMeteredProxy.finish" should {
    "delegate the call" in new TestEnv[Unit] {
      proxyToTest.finish
      verify(writerMock).finish
      verify(counterMock).labels(?)
      verify(counterChildMock).inc(?)
    }
  }
}

object FeedWriterMeteredProxySpec {
  private[FeedWriterMeteredProxySpec] trait TestEnv[T] extends MockitoSupport with FeedPropertiesTest {

    protected val writerMock: FeedWriter[T] = mock[FeedWriter[T]]
    when(writerMock.props).thenReturn(properties)
    when(writerMock.start).thenReturn(writerMock)
    when(writerMock.write(?)).thenReturn(writerMock)
    when(writerMock.finish).thenReturn(writerMock)

    protected val counterMock: Counter = mock[Counter]
    protected val counterChildMock: Counter.Child = mock[Counter.Child]
    doNothing().when(counterMock).inc()
    doNothing().when(counterChildMock).inc(?)
    when(counterMock.labels(?)).thenReturn(counterChildMock)
    protected val prometheusRegistryMock: PrometheusRegistry = mock[PrometheusRegistry]
    when(prometheusRegistryMock.register(?)).thenReturn(counterMock)

    protected val proxyToTest: FeedWriterMeteredProxy[T] =
      new FeedWriterMeteredProxy[T](writerMock, prometheusRegistryMock)
  }
}
