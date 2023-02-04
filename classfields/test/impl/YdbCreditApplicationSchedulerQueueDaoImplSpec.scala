package ru.yandex.vertis.shark.dao.impl

import com.softwaremill.tagging.Tagger
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.shark.dao.CreditApplicationSchedulerQueueDao
import ru.yandex.vertis.shark.model.{CreditApplicationId, Tag}
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isUnit}
import zio.test.TestAspect._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.Instant

object YdbCreditApplicationSchedulerQueueDaoImplSpec extends DefaultRunnableSpec {

  private lazy val daoLayer =
    Clock.any >+> ydb >+> TransactionSupport.live >+> CreditApplicationSchedulerQueueDao.live

  private val creditApplicationId = "creditApplicationId".taggedWith[Tag.CreditApplicationId]
  private val domain = Domain.DOMAIN_AUTO
  private val shard = 2

  private val ts = Instant.now()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    import CreditApplicationSchedulerQueueDao._
    (suite("YdbCreditApplicationSchedulerQueueDaoImpl")(
      testM("list") {
        val actual = transactionally(list(domain, 2, ts, 100))
        assertM(actual)(
          equalTo(ListResult(Seq.empty[CreditApplicationId], Some(0)))
        )
      },
      testM("upsert") {
        val actual = transactionally(upsert(domain, shard, creditApplicationId, ts).repeatN(4))
        assertM(actual)(isUnit)
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(daoLayer)
  }
}
