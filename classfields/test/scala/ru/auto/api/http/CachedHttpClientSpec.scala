package ru.auto.api.http

import org.apache.http.HttpVersion
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.DefaultHttpResponseFactory
import ru.auto.api.testkit.MockHttpClientImpl

import scala.concurrent.Future
import org.scalacheck.Gen
import ru.auto.api.services.HttpClientSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.auto.api.app.redis.RedisCodecs._
import ru.auto.api.app.redis.{RedisCache, RedisCacheBuilder}
import ru.auto.api.auth.Application
import ru.auto.api.model.RequestParams
import ru.auto.api.util.{Request, RequestImpl}

import scala.concurrent.duration._

class CachedHttpClientSpec extends HttpClientSpec with MockitoSupport {
  private val redisCache = mock[RedisCache]

  protected val http: MockHttpClientImpl with CachedHttpClient = new MockHttpClientImpl with CachedHttpClient {

    val redisCacheBuilder: RedisCacheBuilder = {
      val builder = mock[RedisCacheBuilder]

      when(builder.getClient(?)).thenReturn(redisCache)

      builder
    }
  }

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.next)))
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r
  }

  "CachedHttpClientSpec" should {
    "Use data from cache" in {
      val res = DefaultHttpResponseFactory.INSTANCE
        .newHttpResponse(HttpVersion.HTTP_1_1, 200, null)
      res.setEntity(new StringEntity("CachedData", ContentType.TEXT_PLAIN))

      http.respondWith("Data")
      val url = new URIBuilder("/someUrl")
      val req = new HttpGet(url.toString)

      when(redisCache.get[Long](?)(?, ?)).thenReturn(Future.successful(Option(4.toLong)))
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.unit)
      val cacheProps = Cache[Long](10.minutes, "some key")
      http
        .doRequest("method", req, cacheProps = cacheProps)(r => r.getEntity.getContentLength)
        .await

      http.respondWith("DataSecond")

      val result = http
        .doRequest("method", req, cacheProps = cacheProps)(r => r.getEntity.getContentLength)
        .await

      result shouldBe "Data".length
    }
  }

  "Create cache per method" in {
    val res = DefaultHttpResponseFactory.INSTANCE
      .newHttpResponse(HttpVersion.HTTP_1_1, 200, null)
    res.setEntity(new StringEntity("CachedData", ContentType.TEXT_PLAIN))

    val cacheProps = Cache[Long](10.minutes, "some key")
    http.respondWith("Data")

    val url = new URIBuilder("/someUrl")
    val req = new HttpGet(url.toString)

    when(redisCache.get(?)(?, ?)).thenReturn(Future.successful(None))
    when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.unit)

    http
      .doRequest("method", req, cacheProps = cacheProps)(r => r.getEntity.getContentLength)
      .await

    http
      .doRequest("anotherMethod", req, cacheProps = cacheProps)(r => r.getEntity.getContentLength)
      .await

    http.caches.size shouldBe 2
  }

  "Not duplicate caches" in {
    val res = DefaultHttpResponseFactory.INSTANCE
      .newHttpResponse(HttpVersion.HTTP_1_1, 200, null)
    res.setEntity(new StringEntity("CachedData", ContentType.TEXT_PLAIN))

    when(redisCache.get(?)(?, ?)).thenReturn(Future.successful(None))
    when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.unit)

    http.respondWith("Data")
    val cacheProps = Cache[Long](10.minutes, "some key")

    http.caches.clear()
    val url = new URIBuilder("/someUrl")
    val req = new HttpGet(url.toString)

    http
      .doRequest("method", req, cacheProps = cacheProps)(r => r.getEntity.getContentLength)
      .await

    http
      .doRequest("method", req, cacheProps = cacheProps)(r => r.getEntity.getContentLength)
      .await

    http.caches.size shouldBe 1
  }
}
