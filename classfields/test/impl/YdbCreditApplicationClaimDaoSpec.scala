package ru.yandex.vertis.shark.dao.impl

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import common.zio.ydb.testkit.InitSchema
import ru.yandex.vertis.shark.dao.CreditApplicationClaimDao
import ru.yandex.vertis.shark.model._
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isTrue}
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object YdbCreditApplicationClaimDaoSpec extends DefaultRunnableSpec {

  private lazy val creditApplicationClaimDaoLayer =
    Clock.any >+> ydb >+> TransactionSupport.live >+> CreditApplicationClaimDao.live

  private val creditApplicationId = "creditApplicationId".taggedWith[Tag.CreditApplicationId]
  private val claimId = "claimId".taggedWith[Tag.CreditApplicationClaimId]

  override def spec: ZSpec[TestEnvironment, Any] = {
    import CreditApplicationClaimDao._

    (suite("YdbCreditApplicationClaimDaoImpl")(
      testM("upsert") {
        val res = transactionally(upsert(claimId, creditApplicationId))
        assertM(res.as(true))(
          isTrue
        )
      },
      testM("get") {
        val res = transactionally(get(claimId))
        assertM(res)(
          equalTo(creditApplicationId.some)
        )
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie))
      .provideCustomLayerShared(creditApplicationClaimDaoLayer)
  }
}
