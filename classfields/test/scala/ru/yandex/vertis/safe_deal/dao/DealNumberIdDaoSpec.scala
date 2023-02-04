package ru.yandex.vertis.safe_deal.dao

import com.softwaremill.tagging._
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.safe_deal.model._
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isTrue}
import zio.test.TestAspect.{beforeAll, sequential}
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object DealNumberIdDaoSpec extends DefaultRunnableSpec {

  private lazy val dealNumberDaoLayer =
    Clock.any >+> ydb >+> TransactionSupport.live >+> DealNumberIdDao.live

  private val dealId: DealId = "123".taggedWith[Tag.DealId]
  private val dealNumber: DealNumber = 123L.taggedWith[Tag.DealNumber]

  override def spec: ZSpec[TestEnvironment, Any] = {
    import ru.yandex.vertis.safe_deal.dao.DealNumberIdDao._

    (suite("YdbDealNumberIdDaoImpl")(
      testM("add") {
        val res = transactionally(add(dealNumber, dealId)).as(true)
        assertM(res)(isTrue)
      },
      testM("get") {
        val res = transactionally(list(List(dealNumber)))
        assertM(res)(equalTo(List(dealId)))
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(dealNumberDaoLayer)
  }
}
