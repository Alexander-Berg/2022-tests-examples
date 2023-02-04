package ru.yandex.vertis.moderation.scheduler.task.fullscan

import cats.effect.{ContextShift, IO}
import org.junit.runner.RunWith
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Minutes, Span}
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, CompositeFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.impl.OwnerEnrichmentInstanceDao
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.InstanceDaoSerDe
import ru.yandex.vertis.moderation.dao.impl.ydb.{YdbInstanceDao, YdbInstanceDaoSchema, YdbOwnerDao}
import ru.yandex.vertis.moderation.dao.{FuturedInstanceDao, FuturedOwnerDao, TransformingInstanceDao}
import ru.yandex.vertis.moderation.feature.ModerationFeatureTypes
import ru.yandex.vertis.moderation.model.instance.{Instance, User}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.scheduler.task.fullscan.FullScanTask.FullScanResult
import ru.yandex.vertis.moderation.service.impl.OwnerServiceImpl
import ru.yandex.vertis.moderation.service.{InstanceTransformer, OwnerService}
import ru.yandex.vertis.quality.ydb_utils.config.YdbConfig
import ru.yandex.vertis.quality.ydb_utils.factory.YdbFactory
import ru.yandex.vertis.quality.ydb_utils.{DefaultYdbWrapper, WithTransaction}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class StressTestSpec extends SpecBase {
  implicit val global: ExecutionContext = ExecutionContext.global
  implicit val ydbCs: ContextShift[IO] = IO.contextShift(global)
  val token = "INSERT_KEY_HERE"

  lazy val ydbWrapper: DefaultYdbWrapper[IO] =
    YdbFactory[IO](
      YdbConfig(
        endpoint = "ydb-ru-prestable.yandex.net:2135",
        database = "/ru-prestable/verticals/testing/common",
        tablePrefix = "/ru-prestable/verticals/testing/common/moderation/instances/autoru",
        sessionAcquireTimeout = 10.second,
        token = token
      )
    )

  val serDe = new InstanceDaoSerDe(Service.AUTORU)

  lazy val ydbWrapperOwner: DefaultYdbWrapper[IO] =
    YdbFactory[IO](
      YdbConfig(
        endpoint = "ydb-ru-prestable.yandex.net:2135",
        database = "/ru-prestable/verticals/testing/common",
        tablePrefix = "/ru-prestable/verticals/testing/common/moderation/owners/autoru",
        sessionAcquireTimeout = 10.second,
        token = token
      )
    )

  val ownerDao =
    new FuturedOwnerDao[IO, User](
      new YdbOwnerDao[IO, WithTransaction[IO, *]](
        ydbWrapperOwner,
        new InstanceDaoSerDe(Service.AUTORU),
        None
      )
    )

  lazy val instanceDaoF =
    new YdbInstanceDao[WithTransaction[IO, *]](
      ydbWrapper,
      new YdbInstanceDaoSchema(serDe, None, None),
      serDe,
      Service.AUTORU
    )

  lazy val instanceDao: FuturedInstanceDao[IO] =
    new FuturedInstanceDao[IO](instanceDaoF) with OwnerEnrichmentInstanceDao with TransformingInstanceDao {
      implicit override def ec: ExecutionContext = ExecutionContext.global

      implicit override def contextShift: ContextShift[IO] = ydbCs

      override def ownerService: OwnerService = new OwnerServiceImpl(ownerDao)(ExecutionContext.global)

      override def instanceTransformer: InstanceTransformer = InstanceTransformer.forService(Service.AUTORU)(features)

    }

  val features: FeatureRegistry =
    new InMemoryFeatureRegistry(new CompositeFeatureTypes(Iterable(BasicFeatureTypes, ModerationFeatureTypes)))

  val task: StatelessBatchedFullScanTask =
    new StatelessBatchedFullScanTask(instanceDao)(global, scheduler, features) {
      override protected def batchedAction(instances: Seq[Instance]): Future[Unit] = Future.successful(())

      override protected def name: String = "test-task"

      var counter: Option[Long] = None

      override protected def onCount(acc: FullScanResult): Unit = {
        import scala.concurrent.duration._
        counter match {
          case Some(value) =>
            val timeSpent = (System.currentTimeMillis() - value).millis
            val rps = acc.total / Math.max(timeSpent.toSeconds, 1)
            Console.println(s"Task $name progress [${acc.total} in $timeSpent  --- $rps i/sec]")
          case None => counter = Some(System.currentTimeMillis())
        }

      }
    }

  "StressTest" should {
    "run once" ignore {
      features
        .updateFeature(
          "test-task-options",
          "{\"batchSize\":1000,\"batchesPerMinute\":300,\"countEveryBatch\":10}",
          None,
          None
        )
        .futureValue
      task.run.futureValue(Timeout(Span(10, Minutes)))
    }
  }
}
