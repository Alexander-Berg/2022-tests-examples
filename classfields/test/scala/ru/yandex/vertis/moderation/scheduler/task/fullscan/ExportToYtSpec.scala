package ru.yandex.vertis.moderation.scheduler.task.fullscan

import cats.effect.{ContextShift, IO}
import org.junit.runner.RunWith
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Minutes, Span}
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, CompositeFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.client.{DelegateYtClient, PrometheusMeteredYtClient, YtClient}
import ru.yandex.vertis.moderation.config.HttpYtClientConfig
import ru.yandex.vertis.moderation.dao.factory.YtClientFactory
import ru.yandex.vertis.moderation.dao.impl.OwnerEnrichmentInstanceDao
import ru.yandex.vertis.moderation.dao.impl.empty.EmptyOwnerDao
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.InstanceDaoSerDe
import ru.yandex.vertis.moderation.dao.impl.ydb.{YdbInstanceDao, YdbInstanceDaoSchema, YdbOwnerDao}
import ru.yandex.vertis.moderation.dao.{FuturedInstanceDao, FuturedOwnerDao, TransformingInstanceDao}
import ru.yandex.vertis.moderation.feature.ModerationFeatureTypes
import ru.yandex.vertis.moderation.model.MaybeExpiredInstance
import ru.yandex.vertis.moderation.model.instance.{ExternalId, User}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.service.impl.OwnerServiceImpl
import ru.yandex.vertis.moderation.service.{InstanceTransformer, OwnerService}
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.quality.ydb_utils.config.YdbConfig
import ru.yandex.vertis.quality.ydb_utils.factory.YdbFactory
import ru.yandex.vertis.quality.ydb_utils.{DefaultYdbWrapper, WithTransaction}
import ru.yandex.vertis.scheduler.model.Payload.Async

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ExportToYtSpec extends SpecBase {
  implicit val global: ExecutionContext = ExecutionContext.global
  implicit val ydbCs: ContextShift[IO] = IO.contextShift(global)
  val token = "YDB_TOKEN"
  val service = Service.REALTY

  lazy val ydbWrapper: DefaultYdbWrapper[IO] =
    YdbFactory[IO](
      YdbConfig(
        endpoint = "ydb-ru-prestable.yandex.net:2135",
        database = "/ru-prestable/verticals/testing/common",
        tablePrefix = s"/ru-prestable/verticals/testing/common/moderation/instances/${service.toString.toLowerCase}",
        sessionAcquireTimeout = 10.second,
        token = token
      )
    )

  val serDe = new InstanceDaoSerDe(service)

  lazy val ydbWrapperOwner: DefaultYdbWrapper[IO] =
    YdbFactory[IO](
      YdbConfig(
        endpoint = "ydb-ru-prestable.yandex.net:2135",
        database = "/ru-prestable/verticals/testing/common",
        tablePrefix = s"/ru-prestable/verticals/testing/common/moderation/owners/${service.toString.toLowerCase}",
        sessionAcquireTimeout = 10.second,
        token = token
      )
    )

  val ownerDao =
    new FuturedOwnerDao[IO, User](
      new YdbOwnerDao[IO, WithTransaction[IO, *]](
        ydbWrapperOwner,
        new InstanceDaoSerDe(service),
        None
      )
    )

  lazy val instanceDaoF =
    new YdbInstanceDao[WithTransaction[IO, *]](
      ydbWrapper,
      new YdbInstanceDaoSchema(serDe, None, None),
      serDe,
      service
    )

  lazy val instanceDao: FuturedInstanceDao[IO] =
    new FuturedInstanceDao[IO](instanceDaoF) with OwnerEnrichmentInstanceDao with TransformingInstanceDao {
      implicit override def ec: ExecutionContext = ExecutionContext.global

      implicit override def contextShift: ContextShift[IO] = ydbCs

      override def ownerService: OwnerService = new OwnerServiceImpl(ownerDao)(ExecutionContext.global)

      override def instanceTransformer: InstanceTransformer = InstanceTransformer.forService(service)(features)

      override def getAllMaybeExpired(fromOpt: Option[ExternalId]): fs2.Stream[IO, MaybeExpiredInstance] =
        super.getAllMaybeExpired(fromOpt).take(1000000)

    }

  lazy val features: FeatureRegistry =
    new InMemoryFeatureRegistry(new CompositeFeatureTypes(Iterable(BasicFeatureTypes, ModerationFeatureTypes)))

  lazy val ytClient: YtClient =
    new DelegateYtClient(
      YtClientFactory(
        HttpYtClientConfig(
          proxyHost = "hahn.yt.yandex.net",
          token = "YT_TOKEN"
        )
      )
    ) with PrometheusMeteredYtClient {
      implicit override def opsContext: ExecutionContext = ExecutionContext.global

      override protected def prometheusMeteringRegistry: PrometheusRegistry = prometheusRegistry

      override protected def module: String = "test"

      override def scope: Service = service
    }

  val task: YtExportTask =
    new YtExportTask(
      instanceDao,
      ytClient,
      YtExportTask.YtExportConfig(
        ytPathPrefix = "//home/verticals/export/ydb/moderation/test/instances",
        ttl = 1.day
      )
    )

  "StressTest" should {
    "run once" ignore {
      task.toManualTask.payload.asInstanceOf[Async].run().futureValue(Timeout(Span(10, Minutes)))
    }
  }
}
