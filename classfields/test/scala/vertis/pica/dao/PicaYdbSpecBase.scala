package vertis.pica.dao

import common.clients.avatars.AvatarsClient
import common.zio.logging.Logging
import ru.yandex.vertis.pica.model.model
import ru.yandex.vertis.ydb.Ydb
import ru.yandex.vertis.ydb.zio.Tx
import vertis.pica.Namespace
import vertis.pica.conf.{PicaCoreNamespaceConfig, ReschedulingPolicyConfig, ThrottlingConfig}
import vertis.pica.model.{Namespaces, Url}
import vertis.pica.service.avatars.{AvatarsService, AvatarsServiceImpl}
import vertis.pica.util.PartitioningUtils
import vertis.ydb.partitioning.manual.ManualPartition
import vertis.ydb.test.YdbTest
import vertis.zio.ratelimiter.RateLimiterConfig
import vertis.zio.test.ZioSpecBase
import zio._
import zio.clock.Clock
import zio.duration._

/** @author ruslansd
  */
trait PicaYdbSpecBase extends YdbTest {
  this: org.scalatest.Suite with ZioSpecBase =>

  protected def throttlingConfig: ThrottlingConfig = ThrottlingConfig(
    RateLimiterConfig(50),
    RateLimiterConfig(50)
  )

  protected val testNamespace: Namespace = Namespaces.OYandex
  protected val protoNamespace: model.Namespace = Namespaces.convertNamespaceToProto(testNamespace)

  private val rescheduling = ReschedulingPolicyConfig(
    1.seconds,
    5.seconds,
    2,
    3,
    5.seconds
  )

  protected val namespaceConf: PicaCoreNamespaceConfig =
    PicaCoreNamespaceConfig(testNamespace, false, false, throttlingConfig, rescheduling, partitionBits = 3)

  protected def getPartition(url: Url): ManualPartition =
    PartitioningUtils.partitionByHost(namespaceConf.partitioning)(url.host)

  protected lazy val storage =
    new ImageStorage(ydbWrapper, prometheusRegistry, namespaceConf.partitioning)

  protected lazy val queueStorage = new QueueImpl(ydbWrapper, prometheusRegistry, namespaceConf.partitioning)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    runSync(storage.init *> queueStorage.init)
    ()
  }

  protected def runTx[R <: Clock, E, T](tx: Tx[R with Ydb, E, T]): ZIO[R, E, T] =
    Ydb.runTx(tx).provideSomeLayer[R](storage.ydbLayer)

  protected def createAvatarsService(avatarsClient: AvatarsClient.Service): URIO[Logging.Logging, AvatarsService] =
    ZIO
      .service[Logging.Service]
      .map(new AvatarsServiceImpl(avatarsClient, testNamespace.toString, _))
}
