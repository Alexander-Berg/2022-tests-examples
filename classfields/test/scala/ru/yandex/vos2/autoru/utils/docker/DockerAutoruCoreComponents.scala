package ru.yandex.vos2.autoru.utils.docker

import com.yandex.ydb.core.grpc.GrpcTransport
import com.yandex.ydb.table.TableClient
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc
import io.prometheus.client.{Collector, CollectorRegistry}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.BoundedExponentialBackoffRetry
import org.apache.curator.test.TestingCluster
import ru.yandex.common.monitoring.CompoundHealthCheckRegistry
import ru.yandex.vertis.baker.env.Env
import ru.yandex.vertis.baker.lifecycle.Application
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.mockito.MockitoSupport.mock
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, ReportingTracingSupport, TracingSupport}
import ru.yandex.vertis.ydb.skypper.YdbWrapper
import ru.yandex.vos2.autoru.components.DefaultAutoruCoreComponents
import ru.yandex.vos2.autoru.dao.drafts.{DraftDao, YdbDraftDao}
import ru.yandex.vos2.autoru.model.extdata.{ResellerDeactivationParams, VinAndLicensePlateRequiredRegions}
import ru.yandex.vos2.autoru.services.chat.ChatClient
import ru.yandex.vos2.autoru.services.mds.MdsUploader
import ru.yandex.vos2.autoru.utils.TestDataEngine
import ru.yandex.vos2.commonfeatures.FeatureRegistryFactory
import ru.yandex.vos2.extdata.ExtDataEngine
import ru.yandex.vos2.services.passport.PassportClient

import java.net.URL
import java.util.function.Predicate
import scala.concurrent.duration._

object DockerAutoruCoreComponents extends DefaultAutoruCoreComponents {

  //  override lazy val operational: OperationalSupport = OpsFactory.newOperationalSupport(AppConfig.createDefault())
  override lazy val chatClient: ChatClient = mock[ChatClient]

  override lazy val resellerDeactivationParams: ResellerDeactivationParams = mock[ResellerDeactivationParams]

  override lazy val operational: OperationalSupport = new OperationalSupport {
    override def healthChecks: CompoundHealthCheckRegistry = new CompoundHealthCheckRegistry

    implicit override def codahaleRegistry: CodahaleRegistry = ???

    implicit override def prometheusRegistry: PrometheusRegistry = new PrometheusRegistry {
      override def asCollectorRegistry(): CollectorRegistry = ???

      override def register[C <: Collector](c: C): C = c

      override def unregister(predicate: Predicate[Collector]): Unit = ???
    }
  }
  override lazy val extDataEngine: ExtDataEngine = TestDataEngine

  override lazy val vinGrzRequiredRegions = VinAndLicensePlateRequiredRegions(Set(213L))

  lazy val zookeeper: TestingCluster = {
    val x = new TestingCluster(1)
    DockerAutoruCoreComponentsBuilder.registerToClose(x)
    x.start()
    x
  }

  override lazy val featureRegistry: FeatureRegistry = FeatureRegistryFactory.inMemory()

  override lazy val defaultApp: Application = new Application {
    override def env: Env = new Env(DockerEnvProvider)
  }

  override lazy val skypper: YdbWrapper = {
    val conf = env.serviceConfig.getConfig("ydb2")
    val database = conf.getString("database")
    val host = conf.getString("host")
    val port = conf.getInt("port")

    val transport: GrpcTransport = GrpcTransport
      .forHost(host, port)
      .build

    lazy val rpc: GrpcTableRpc = GrpcTableRpc.ownTransport(transport)
    lazy val tableClient: TableClient = TableClient.newClient(rpc).build()
    YdbWrapper("test", tableClient, tablePrefix = "/local")(ec)
  }

  override lazy val draftSkypper: YdbWrapper = skypper

  override lazy val draftDao: DraftDao = {
    val dao = new YdbDraftDao(skypper)(ec)
    dao
  }

  override lazy val zkCommonClient: CuratorFramework = {
    val retryPolicy = new BoundedExponentialBackoffRetry(10000, 60000, 29)
    val zkClient = CuratorFrameworkFactory
      .builder()
      .namespace(env.serviceName + "/autoru")
      .connectString(zookeeper.getConnectString)
      .sessionTimeoutMs(15.seconds.toMillis.toInt)
      .connectionTimeoutMs(5.seconds.toMillis.toInt)
      .retryPolicy(retryPolicy)
      .build()

    zkClient.start()
    zkClient
  }

  override lazy val zkClient: CuratorFramework = zkCommonClient

  override lazy val mdsUploader: MdsUploader = mock[MdsUploader]

  override lazy val passportClient: PassportClient = mock[PassportClient]

  override lazy val tracing: TracingSupport = LocalTracingSupport(
    EndpointConfig("component", "localhost", 36240)
  )
}
