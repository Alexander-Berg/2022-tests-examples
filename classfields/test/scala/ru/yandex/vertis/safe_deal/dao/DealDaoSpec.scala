package ru.yandex.vertis.safe_deal.dao

import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.timestamp._
import com.softwaremill.tagging.Tagger
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.safe_deal.model.Tag
import ru.yandex.vertis.safe_deal.proto.common.{DealState, DealStep}
import ru.yandex.vertis.safe_deal.proto.{model => proto}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.AutoUser
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import ru.yandex.vertis.zio_baker.zio.token_distributor.config.TokenDistributorConfig
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isTrue}
import zio.test.TestAspect.{beforeAll, sequential}
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.ZLayer

import java.time.Instant
import java.time.temporal.ChronoUnit

object DealDaoSpec extends DefaultRunnableSpec {

  private lazy val dealDaoLayer =
    Clock.any >+> ydb >+> TransactionSupport.live >+> ZLayer.succeed(
      TokenDistributorConfig.apply(1, Domain.DOMAIN_AUTO.name)
    ) >+> DealDao.live

  private val dealId = "dealId".taggedWith[Tag.DealId]
  private val user = AutoUser("user:1".taggedWith[zio_baker.Tag.UserId])
  private val buyer = proto.Deal.Buyer(user.toPlain).some
  private val timestamp: Timestamp = Timestamp(Instant.now.getEpochSecond - 1)

  private val deal = proto.Deal(
    id = dealId,
    buyer = buyer,
    domain = Domain.DOMAIN_AUTO,
    created = timestamp.some,
    updated = timestamp.some,
    state = DealState.DRAFT,
    scheduledAt = timestamp.some
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    import DealDao._

    (suite("YdbDealDaoImpl")(
      testM("upsert") {

        val res = transactionally(upsert(deal))
        assertM(res.as(true))(isTrue)
      },
      testM("list for search") {
        val res = transactionally(
          list(
            ByParams(
              domain = Domain.DOMAIN_AUTO,
              limit = 10,
              offset = 0,
              buyerId = user.some,
              userFilterMode = UserFilterModeIntersect,
              steps = Seq(
                DealStep.DEAL_CREATED,
                DealStep.DEAL_INVITE_ACCEPTED,
                DealStep.DEAL_CONFIRMED,
                DealStep.DEAL_COMPLETING,
                DealStep.DEAL_COMPLETED,
                DealStep.DEAL_CANCELLING,
                DealStep.DEAL_CANCELLED,
                DealStep.DEAL_DECLINED,
                DealStep.DEAL_STEP_UNKNOWN
              )
            )
          )
        )
        assertM(res)(equalTo(ListResult(Seq(deal), 1L.some)))
      },
      testM("list for scheduler") {
        val res = transactionally(
          list(
            ForProcess(
              domain = Domain.DOMAIN_AUTO,
              shards = Seq(0),
              limit = 10,
              Instant.now.plus(1, ChronoUnit.HOURS)
            )
          )
        )
        assertM(res)(equalTo(ListResult(Seq(deal), 1L.some)))
      },
      testM("get") {
        val res = transactionally(get(GetById(dealId)))
        assertM(res)(equalTo(deal.some))
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(dealDaoLayer)

  }
}
