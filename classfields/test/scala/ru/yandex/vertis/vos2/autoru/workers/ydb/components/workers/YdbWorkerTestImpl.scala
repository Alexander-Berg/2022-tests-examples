package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, ThreadPoolExecutor}

import com.google.common.util.concurrent.RateLimiter
import ru.yandex.vertis.baker.components.workdistribution.{WorkDistributionData, WorkerToken}
import ru.yandex.vertis.baker.components.workersfactory.workers.WorkersFactory
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.tracing.TracingSupport
import ru.yandex.vos2.workers.workdistribution.WorkersTokens
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.worker.YdbWorkerImpl
import ru.yandex.vertis.ydb.skypper.YdbWrapper
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.commonfeatures.FeaturesManager

import scala.concurrent.ExecutionContext

trait YdbWorkerTestImpl extends YdbWorkerImpl {
  override def workersCount: Int = 0

  override def workersFactory: WorkersFactory = ???

  implicit override def ec: ExecutionContext = ???

  override def features: FeaturesManager = null

  override def operational: OperationalSupport = ???

  override def token: WorkerToken = WorkersTokens.MultipostingPreActivationWorkerYdbWorkerToken

  override def shouldWork: Boolean = ???

  override val pool: ThreadPoolExecutor = Executors.newFixedThreadPool(1).asInstanceOf[ThreadPoolExecutor]

  override def getOffersRateLimiter: RateLimiter = ???

  override def ydb: YdbWrapper = ???
  override def offerDaoYdb: AutoruOfferDao = null
  override def offerDaoVos: AutoruOfferDao = null
  override def tokensDistribution: AtomicReference[Option[WorkDistributionData]] = null
  override def trsSupport: TracingSupport = null
}
