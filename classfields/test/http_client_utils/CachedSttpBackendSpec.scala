package ru.yandex.vertis.vsquality.utils.http_client_utils

import cats.syntax.applicative._
import com.softwaremill.tagging._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{spy, times, verify, verifyNoMoreInteractions}
import org.typelevel.ci.CIString
import ru.yandex.vertis.vsquality.utils.cached_utils.config.MethodCachingConfig.{Disabled, Enabled}
import ru.yandex.vertis.vsquality.utils.cached_utils.config.{CacheConfig, MethodCachingConfig}
import ru.yandex.vertis.vsquality.utils.cached_utils.factory.CacheFactory
import ru.yandex.vertis.vsquality.utils.cached_utils.model._
import ru.yandex.vertis.vsquality.utils.cached_utils._
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.cats_utils.MonadErr
import ru.yandex.vertis.vsquality.utils.http_client_utils.HttpClientUtils.{
  ContentTypes,
  DelegateSttpBackend,
  RichResponse,
  SttpOps
}
import ru.yandex.vertis.vsquality.utils.http_client_utils.cached.CachedSttpBackend
import ru.yandex.vertis.vsquality.utils.http_client_utils.cached.CachedSttpBackend.SttpNamed
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase
import sttp.client3._
import sttp.client3.quick.quickRequest
import sttp.model.{Header, Uri}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class CachedSttpBackendSpec extends SpecBase {

  private class TestContext {

    protected val mockedBackend: SttpBackend[F, Any] =
      mock[SttpBackend[F, Any]]

    stub(mockedBackend.send(_: Request[Array[Byte], Any])) { case _: Request[Array[Byte], Any] =>
      Response.ok(Array.empty[Byte]).copy(headers = List(Header("random", "header"))).pure[F]
    }

    protected val cache: CacheF[F, HttpBytesRequest, SerializableHttpBytesResponse] =
      spy(
        new CacheF(CacheFactory(CacheConfig.InMemory, Layouts.SttpBackendInfoLayout))
      )

    implicit protected val cachedBackend: SttpBackend[F, Any] =
      new DelegateSttpBackend[F](mockedBackend) with CachedSttpBackend[F, Any] {
        implicit override def F: MonadErr[F] = implicitly
        override protected def backendCache: BackendCache[F] = cache

        override protected def methodCachingConfigs: Map[MethodNameTag, MethodCachingConfig] =
          Map(
            "get-disabled".taggedWith[Tags.MethodName] -> Disabled,
            "get-enabled".taggedWith[Tags.MethodName] -> Enabled(10.minutes),
            "get-with-request-id".taggedWith[Tags.MethodName] -> Enabled(
              headersExcludedFromCacheKey = Set(CIString("X-request-Id"))
            ),
            "get-with-custom-headers".taggedWith[Tags.MethodName] -> Enabled(
              headersExcludedFromCacheKey = Set(
                CIString("custom-1"),
                CIString("custom-3")
              )
            )
          )
      }
  }

  private def requestContainsHeader(name: String): Request[Array[Byte], Any] =
    ArgumentMatchers.argThat((_: Request[Array[Byte], Any]).headers.exists(_.name == name))

  private def requestDoesntContainHeader(name: String): Request[Array[Byte], Any] =
    ArgumentMatchers.argThat(!(_: Request[Array[Byte], Any]).headers.exists(_.name == name))

  "CachedSttpBackend" should {
    "not use cache with default request" in new TestContext {
      def sendDefaultRequest: F[Response[Array[Byte]]] =
        quickRequest
          .get(Uri("default"))
          .acceptProto()
          .named("get-default")
          .send(cachedBackend)
          .ensureSuccessCode()

      sendDefaultRequest.await
      verify(mockedBackend).send(?)
      sendDefaultRequest.await
      verify(mockedBackend, times(2)).send(?)
      verify(cache, times(0)).set(?, ?, ?)
    }

    "not use cache if explicitly disabled" in new TestContext {
      def sendRequestWithoutCache: F[Response[Array[Byte]]] =
        quickRequest
          .get(Uri("disabled"))
          .acceptProto()
          .named("get-disabled")
          .send(cachedBackend)
          .ensureSuccessCode()

      sendRequestWithoutCache.await
      verify(mockedBackend).send(?)
      sendRequestWithoutCache.await
      verify(mockedBackend, times(2)).send(?)
      verify(cache, times(0)).set(?, ?, ?)
    }

    "use cache with specified ttl" in new TestContext {
      def sendCachedRequest: F[Response[Array[Byte]]] =
        quickRequest
          .get(Uri("enabled"))
          .acceptProto()
          .named("get-enabled")
          .send(cachedBackend)
          .ensureSuccessCode()

      sendCachedRequest.await
      verify(mockedBackend).send(?)
      sendCachedRequest.await
      verifyNoMoreInteractions(mockedBackend)
      verify(cache).set(?, ?, ArgumentMatchers.eq(10.minutes))
    }

    "remove x-request-id from cache key by default" in new TestContext {
      val xRequestId = "X-request-ID"
      def sendRequestWithRequestId: F[Response[Array[Byte]]] =
        quickRequest
          .get(Uri("requestId"))
          .acceptProto()
          .header(xRequestId, "123")
          .named("get-with-request-id")
          .send(cachedBackend)
          .ensureSuccessCode()

      sendRequestWithRequestId.await

      // header was sent
      verify(mockedBackend).send(requestContainsHeader(xRequestId))

      // key did not contain header
      verify(cache).set(requestDoesntContainHeader(xRequestId), ?, ?)
    }

    "remove specified headers from cache key" in new TestContext {
      val xRequestId = "X-REQUEST-ID"
      val header1 = "custom-1"
      val header2 = "custom-2"
      val header3 = "custom-3"

      def sendRequestWithRequestId: F[Response[Array[Byte]]] =
        quickRequest
          .get(Uri("requestId"))
          .acceptProto()
          .header(xRequestId, "123")
          .header(header1, "123")
          .header(header2, "1234")
          .header(header3, "123")
          .named("get-with-custom-headers")
          .send(cachedBackend)
          .ensureSuccessCode()

      sendRequestWithRequestId.await
      Seq(xRequestId, header1, header2, header3).foreach { header =>
        verify(mockedBackend).send(requestContainsHeader(header))
      }

      verify(cache).set(requestDoesntContainHeader(header1), ?, ?)
      verify(cache).set(requestDoesntContainHeader(header3), ?, ?)

      verify(cache).set(requestContainsHeader(xRequestId), ?, ?)
      verify(cache).set(requestContainsHeader(header2), ?, ?)
    }
  }
}
