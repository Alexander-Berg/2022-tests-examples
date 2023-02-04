package ru.yandex.vertis.shark.dao.impl

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.shark.dao.CreditApplicationSchedulerUpdateRequestDao
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isUnit}
import zio.test.TestAspect._
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.Instant

object YdbCreditApplicationSchedulerUpdateRequestDaoImplSpec extends DefaultRunnableSpec {

  private lazy val daoLayer =
    TestClock.any >+> ydb >+> TransactionSupport.live >+> CreditApplicationSchedulerUpdateRequestDao.live

  private val creditApplicationId = "creditApplicationId".taggedWith[Tag.CreditApplicationId]
  private val requestId = "someRequestId"
  private val updateRequest = CreditApplication.UpdateRequest(atMostOnce = true, timestamp = Instant.now)

  override def spec: ZSpec[TestEnvironment, Any] = {
    import CreditApplicationSchedulerUpdateRequestDao._
    (suite("YdbCreditApplicationSchedulerUpdateRequestDaoImpl")(
      testM("upsert") {
        val actual = upsert(creditApplicationId, updateRequest, requestId.some).repeatN(4)
        assertM[CreditApplicationSchedulerUpdateRequestDao with Clock, Throwable, Unit](actual)(isUnit)
      },
      testM("list") {
        val actual = list(creditApplicationId)
        val expected = Seq.fill(5)(updateRequest)
        assertM[CreditApplicationSchedulerUpdateRequestDao with Clock, Throwable, Seq[CreditApplication.UpdateRequest]](
          actual
        )(equalTo(expected))
      },
      testM("clean") {
        val actual = clean(creditApplicationId)
        assertM[CreditApplicationSchedulerUpdateRequestDao with Clock, Throwable, Unit](actual)(isUnit)
      },
      testM("clean when no data") {
        val actual = clean(creditApplicationId)
        assertM[CreditApplicationSchedulerUpdateRequestDao with Clock, Throwable, Unit](actual)(isUnit)
      },
      testM("list") {
        val actual = list(creditApplicationId).map(_.length)
        assertM[CreditApplicationSchedulerUpdateRequestDao with Clock, Throwable, Int](actual)(equalTo(0))
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie))
      .provideCustomLayerShared(daoLayer)
  }
}
