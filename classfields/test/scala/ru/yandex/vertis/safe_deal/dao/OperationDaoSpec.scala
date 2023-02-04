package ru.yandex.vertis.safe_deal.dao

import cats.implicits._
import com.google.protobuf.timestamp._
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.safe_deal.proto.{model => proto}
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isTrue}
import zio.test.TestAspect.{beforeAll, sequential}
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object OperationDaoSpec extends DefaultRunnableSpec {

  private lazy val operationDaoLayer =
    Clock.any >+> ydb >+> TransactionSupport.live >+> OperationDao.live

  private val operation = proto.BankOperationEvent(
    id = "111",
    operationId = "111_0",
    amount = 100,
    timestamp = Timestamp().some,
    drawDate = Timestamp().some,
    paymentPurpose = "purpose",
    uin = "",
    payerBic = "123",
    payerBank = "ПАО СБЕРБАНК",
    payerName = "Иванов Иван Иванович",
    payerAccount = "123",
    payerCorrAccount = "123",
    dealNumber = 100L.some,
    domain = Domain.DOMAIN_AUTO
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    import ru.yandex.vertis.safe_deal.dao.OperationDao._

    (suite("YdbOperationDaoImpl")(
      testM("upsert") {
        val res = transactionally(upsert(operation)).as(true)
        assertM(res)(isTrue)
      },
      testM("list for search") {
        val res = transactionally(byDealNumbers(Seq(100L), Domain.DOMAIN_AUTO))
        assertM(res)(equalTo(ListResult(Seq(operation))))
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(operationDaoLayer)
  }
}
