package ru.yandex.vertis.parsing.util.http.cache

import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.util.EntityUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import ru.auto.api.unification.Unification.CarsUnificationCollection
import ru.yandex.vertis.parsing.clients.{HttpClientSpec, MockHttpClient}
import ru.yandex.vertis.parsing.components.TestApplicationSupport
import ru.yandex.vertis.parsing.components.ehcache.EhcacheSupport
import ru.yandex.vertis.parsing.util.http.HttpRequestContext

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class CachingHttpClientTest extends FunSuite with HttpClientSpec with BeforeAndAfterAll {

  protected val http = new MockHttpClient
    with CachingHttpClient
    with HttpCacheBuildersSupport
    with EhcacheSupport
    with TestApplicationSupport {

    override protected val cacheMap: Map[String, HttpCache] = Map(
      "parse_trucks" -> ehcache("parse_trucks", 100, 1),
      "parse_cars" -> guavaCache("parse_cars", 100)
    )
  }

  override protected def afterAll(): Unit = {
    http.ehcacheManager.close()
    super.afterAll()
  }

  test("caching for get request") {
    http.reset()
    val req = new HttpGet("/wizardParse?from=Parsing&text=test")
    http.expect("GET", "/wizardParse?from=Parsing&text=test")
    http.respondWith(200, "response")

    val ctx: HttpRequestContext = new HttpRequestContext
    http
      .doRequest("parse_cars", req) { res =>
        EntityUtils.toString(res.getEntity)
      }(trace, ctx)
      .futureValue
    assert(!ctx.fromCache)

    assert(http.getCacheKey(req) == "/wizardParse?from=Parsing&text=test")

    val ctx2: HttpRequestContext = new HttpRequestContext
    http
      .doRequest("parse_cars", req) { res =>
        EntityUtils.toString(res.getEntity)
      }(trace, ctx2)
      .futureValue
    assert(ctx2.fromCache)

    /*val ctx3: HttpRequestContext = new HttpRequestContext
    intercept[NullPointerException] {
      cachedHttpClient.doRequest("parse_cars", req) { res =>
        EntityUtils.toString(res.getEntity)
      }(trace, ctx3).futureValue
    }*/
  }

  test("caching for post request") {
    http.reset()
    val payload = getPostRequestEntity
    val req = new HttpPost("/wizardParse?from=Parsing")
    req.setEntity(
      new ByteArrayEntity(payload.toByteArray)
    )
    http.expect("POST", "/wizardParse?from=Parsing")
    http.expectProto(payload)
    http.respondWith(200, "response")

    val ctx: HttpRequestContext = new HttpRequestContext
    http
      .doRequest("parse_cars", req) { res =>
        EntityUtils.toString(res.getEntity)
      }(trace, ctx)
      .futureValue
    assert(!ctx.fromCache)

    assert(http.getCacheKey(req) == "/wizardParse?from=Parsing|80c32fff90997676365d487ee66b3053")

    val ctx2: HttpRequestContext = new HttpRequestContext
    http
      .doRequest("parse_cars", req) { res =>
        EntityUtils.toString(res.getEntity)
      }(trace, ctx2)
      .futureValue
    assert(ctx2.fromCache)
  }

  test("test ehcache") {
    http.reset()
    val req = new HttpGet("/wizardParse?from=Parsing&text=test")
    http.expect("GET", "/wizardParse?from=Parsing&text=test")
    http.respondWith(200, "response")

    val ctx: HttpRequestContext = new HttpRequestContext
    http
      .doRequest("parse_trucks", req) { res =>
        EntityUtils.toString(res.getEntity)
      }(trace, ctx)
      .futureValue
    assert(!ctx.fromCache)

    assert(http.getCacheKey(req) == "/wizardParse?from=Parsing&text=test")

    val ctx2: HttpRequestContext = new HttpRequestContext
    http
      .doRequest("parse_trucks", req) { res =>
        EntityUtils.toString(res.getEntity)
      }(trace, ctx2)
      .futureValue
    assert(ctx2.fromCache)

    /*val ctx3: HttpRequestContext = new HttpRequestContext
    intercept[NullPointerException] {
      cachedHttpClient.doRequest("parse_cars", req) { res =>
        EntityUtils.toString(res.getEntity)
      }(trace, ctx3).futureValue
    }*/
  }

  private def getPostRequestEntity: CarsUnificationCollection = {
    val carUnifyRequest = CarsUnificationCollection.newBuilder()
    val b = carUnifyRequest.addEntriesBuilder()
    b.setRawMark("Mark")
    b.setRawModel("Model")
    b.setRawBodyType("RawBodyType")
    b.setRawDoorsCount("RawDoorsCount")
    b.setRawYear("RawYear")
    b.setRawPower("RawPower")
    b.setRawTransmission("RawTransmission")
    b.setRawDisplacement("RawDisplacement")
    b.setRawEngineType("RawEngineType")
    b.setRawGearType("RawGearType")
    b.setRawIs4Wd("true")
    carUnifyRequest.build()
  }
}
