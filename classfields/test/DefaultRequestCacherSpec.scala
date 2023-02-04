package ru.yandex.vertis.general.common.cache.test

import common.zio.grpc.client.GrpcClient
import general.globe.api.GeoServiceGrpc.{GeoService, METHOD_GET_PARENT_REGIONS}
import general.globe.api.GetParentRegionsResponse._
import general.globe.api.{GetParentRegionsRequest, GetParentRegionsResponse}
import general.globe.model.Region
import ru.yandex.vertis.general.common.cache.RequestCacher
import ru.yandex.vertis.general.common.cache.testkit.RedisTestCache
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import common.zio.logging.Logging
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import ru.yandex.vertis.general.common.cache.scalapb.Marshallers._
import scala.concurrent.duration._

object DefaultRequestCacherSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("DefaultRequestCacher")(
      testM("cache request and get response from cache") {
        val request1 = GetParentRegionsRequest(List(1L, 2L))
        val response1 = GetParentRegionsResponse(Map(1L -> Region(2L)))
        val request2 = GetParentRegionsRequest(List(1L, 2L, 3L))
        val response2 = GetParentRegionsResponse(Map(1L -> Region(3L), 2L -> Region(4L)))
        for {
          _ <- TestGeoService.setGetParentRegionsResponse(_ => ZIO.succeed(response1))
          client <- ZIO.service[GrpcClient.Service[GeoService]]
          initialResponseToRequest1 <- RequestCacher.cached(
            METHOD_GET_PARENT_REGIONS.getFullMethodName,
            request1,
            10.minutes
          )(r => client.call(_.getParentRegions(r)))
          _ <- TestGeoService.setGetParentRegionsResponse(_ => ZIO.succeed(response2))
          responseToRequest2 <- RequestCacher.cached(METHOD_GET_PARENT_REGIONS.getFullMethodName, request2, 10.minutes)(
            r => client.call(_.getParentRegions(r))
          )
          cachedResponseToRequest1 <- RequestCacher.cached(
            METHOD_GET_PARENT_REGIONS.getFullMethodName,
            request1,
            10.minutes
          )(r => client.call(_.getParentRegions(r)))
        } yield assert(initialResponseToRequest1)(equalTo(response1)) &&
          assert(responseToRequest2)(equalTo(response2)) &&
          assert(cachedResponseToRequest1)(equalTo(response1))
      },
      testM("cache request with ttl") {
        val request = GetParentRegionsRequest(List(1L, 2L, 10L))
        val response1 = GetParentRegionsResponse(Map(1L -> Region(15L)))
        val response2 = GetParentRegionsResponse(Map(30L -> Region(15L)))
        for {
          client <- ZIO.service[GrpcClient.Service[GeoService]]
          _ <- TestGeoService.setGetParentRegionsResponse(_ => ZIO.succeed(response1))
          initialResponse <- RequestCacher.cached(METHOD_GET_PARENT_REGIONS.getFullMethodName, request, 4.seconds)(r =>
            client.call(_.getParentRegions(r))
          )
          _ <- TestGeoService.setGetParentRegionsResponse(_ => ZIO.succeed(response2))
          responseFromCache <- RequestCacher.cached(METHOD_GET_PARENT_REGIONS.getFullMethodName, request, 4.seconds)(
            r => client.call(_.getParentRegions(r))
          )
          _ <- zio.clock.sleep(java.time.Duration.ofSeconds(8))
          responseAfterTtl <- RequestCacher.cached(METHOD_GET_PARENT_REGIONS.getFullMethodName, request, 10.minutes)(
            r => client.call(_.getParentRegions(r))
          )
        } yield assert(initialResponse)(equalTo(response1)) &&
          assert(responseFromCache)(equalTo(response1)) &&
          assert(responseAfterTtl)(equalTo(response2))
      }
    ) @@ sequential).provideCustomLayerShared {
      (Logging.live ++ RedisTestCache.managedRedisCache >>> RequestCacher.live) ++
        TestGeoService.layer ++ zio.clock.Clock.live
    }
  }
}
