package infra.profiler_collector.storage.test

import common.zio.clients.clickhouse.http.{ClickHouseClient, ClickHouseClientLive}
import common.zio.clients.clickhouse.http.ClickHouseClientLive.Auth
import common.zio.clients.clickhouse.testkit.TestClickhouse
import common.zio.sttp.Sttp
import common.zio.sttp.endpoint.Endpoint
import infra.profiler_collector.storage.{ClickhouseSampleStorage, MethodsDictionary, SampleStorage}
import zio.{ZIO, ZLayer}
import zio.magic._
import common.tagged._
import common.zio.tagging.syntax._
import infra.profiler_collector.model.Query
import org.testcontainers.containers.ClickHouseContainer
import zio.test._
import zio.test.TestAspect.sequential

import java.time.Instant

object ClickhouseSampleStorageSpec extends DefaultRunnableSpec {

  def spec = suite("ClickhouseSampleStorage")(
    testM("create table") {
      SampleStorage(_.touchTable) *>
        assertCompletesM
    },
    testM("write samples") {
      for {
        samples <- SampleMother.samples(1000)
        _ <- SampleStorage(_.writeSamples(samples))
      } yield assertCompletes
    },
    testM("query some samples") {
      def loadSamples(service: String, mode: String) = {
        SampleStorage(_.querySamples(Query(Instant.EPOCH, Instant.MAX, mode, service, Map.empty)))
      }
      for {
        samples <- ZIO.collectAll(
          Seq(
            loadSamples("test-service", "cpu"),
            loadSamples("test-service", "itimer"),
            loadSamples("test-service", "alloc"),
            loadSamples("test-service", "lock"),
            loadSamples("service2", "cpu"),
            loadSamples("service2", "itimer"),
            loadSamples("service2", "alloc"),
            loadSamples("service2", "lock"),
            loadSamples("service3", "cpu"),
            loadSamples("service3", "itimer"),
            loadSamples("service3", "alloc"),
            loadSamples("service3", "lock")
          )
        )
      } yield assertTrue(samples.exists(_.nonEmpty))
    }
  ).injectCustomShared(
    ClickhouseSampleStorage.layer,
    TestClickhouse.live,
    ZLayer
      .fromService[ClickHouseContainer, Endpoint](cont => Endpoint(cont.getHost, port = cont.getMappedPort(8123)))
      .tagged[ClickHouseClient.Service],
    ZLayer.fromService[ClickHouseContainer, Auth](cont => Auth(cont.getUsername, cont.getPassword)),
    Sttp.live.orDie,
    ClickHouseClientLive.live,
    MethodsDictionary.inMemoryLayer,
    ZLayer.succeed(tag[ClickhouseSampleStorage]("default.samples"))
  ) @@ sequential
}
