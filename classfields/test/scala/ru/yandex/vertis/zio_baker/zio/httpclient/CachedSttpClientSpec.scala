package ru.yandex.vertis.zio_baker.zio.httpclient

import cats.implicits.catsSyntaxOptionId
import ru.yandex.vertis.zio_baker.cached._
import ru.yandex.vertis.zio_baker.cached.config.CacheConfig
import ru.yandex.vertis.zio_baker.zio.httpclient.HttpClientUtils._
import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import sttp.client3.httpclient.zio.SttpClient
import sttp.client3.{quickRequest, Response}
import sttp.model.{Header, Method, StatusCode, Uri}
import zio.duration.durationInt
import zio.test.Assertion.equalTo
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.test.TestAspect.sequential
import zio.ZIO

object CachedSttpClientSpec extends DefaultRunnableSpec {

  private val liveResponse = Response(
    body = """"liveResponse": true}""",
    code = StatusCode.Created,
    statusText = "SUCCESS"
  )

  // @see https://github.com/zio/zio/issues/4809#issuecomment-871767170
  private lazy val clientBackend =
    (MockHttpClientBackend.live(liveResponse) +!+ cacheBackend) >>>
      CachedHttpClientBackend.wrap(cacheConfig) +!+ cacheBackend
  private val cacheConfig = CacheConfig(cacheByDefault = true)

  private lazy val cacheBackend = Cache.async(
    new InMemoryAsyncCache(Layouts.SttpBackendInfoLayout)
  )

  private val uri: Uri = Uri("google.com")

  private val acceptHeader: Header = Header.accept("application/json")

  private val cachedResponse = SerializableResponse(
    body = """{"cachedResponse": true}""",
    code = StatusCode.Ok,
    statusText = "SUCCESS",
    headers = Seq((acceptHeader.name, acceptHeader.value)),
    history = Nil,
    request =
      SerializableRequestMetadata(Method.GET, uri.toJavaUri.toString, Seq((acceptHeader.name, acceptHeader.value)))
  )

  private val namedRequest = quickRequest
    .get(uri)
    .headers(acceptHeader)
    .named("get-json")
    .accept(ContentType.Json)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CachedSttpClient")(
      testM("use cached response") {
        val res = for {
          backend <- ZIO.service[SttpClient.Service]
          cache <- ZIO.service[BackendCache]
          _ <- cache.set(
            namedRequest,
            cachedResponse,
            10.seconds
          )
          response <- req(backend)
          _ <- cache.delete(namedRequest)
        } yield SerializableResponse.to(response)
        assertM(res)(equalTo(cachedResponse)).provideLayer(clientBackend)
      },
      testM("not use empty cache") {
        val res = for {
          backend <- ZIO.service[SttpClient.Service]
          cache <- ZIO.service[BackendCache]
          response <- req(backend)
          _ <- cache.delete(namedRequest)
        } yield response
        assertM(res)(equalTo(liveResponse)).provideLayer(clientBackend)
      },
      testM("caching requests") {
        val res = for {
          backend <- ZIO.service[SttpClient.Service]
          cache <- ZIO.service[BackendCache]
          _ <- cache.delete(namedRequest)
          response <- req(backend)
          cachedResponse <- cache.get(namedRequest)
        } yield (SerializableResponse.to(response), cachedResponse)
        assertM(res)(equalTo((SerializableResponse.to(liveResponse), SerializableResponse.to(liveResponse).some)))
          .provideLayer(clientBackend)
      }
    ) @@ sequential

  private def req(backend: SttpClient.Service): ZIO[Any, Throwable, Response[String]] =
    namedRequest
      .send(backend)
      .absorb
}
