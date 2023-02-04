package ru.yandex.vertis.shark.dao.impl

import java.time._
import cats.syntax.option._
import com.google.protobuf.timestamp.Timestamp
import com.softwaremill.tagging._
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoFormatInstances._
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoSyntax._
import ru.yandex.vertis.shark.dao.CreditApplicationDao
import ru.yandex.vertis.shark.model.Tag
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.model.User
import ru.yandex.vertis.zio_baker.util.DateTimeUtil._
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isTrue}
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, assertTrue, DefaultRunnableSpec, ZSpec}

import scala.concurrent.duration.DurationInt

object YdbCreditApplicationDaoImplSpec extends DefaultRunnableSpec {

  private val shard = 5

  private lazy val creditApplicationDaoLayer =
    Clock.any >+> ydb >+> TransactionSupport.live >+> CreditApplicationDao.live

  private val user = User.fromPlain("auto_10201").get

  private val ts = Instant.now()

  private val creditApplication = proto.CreditApplication.defaultInstance
    .withId("some-id")
    .withState(proto.CreditApplication.State.DRAFT)
    .withScheduledAt(ts.minusDuration(1.day).toProtoMessage)
    .withSchedulerLastUpdate(ts.minusDuration(2.days).toProtoMessage)
    .withCreated(Timestamp.defaultInstance)
    .withDomain(Domain.DOMAIN_AUTO)
    .withUserId(user.toPlain)
    .withUpdated(Timestamp.defaultInstance)

  private val absentStates = {
    import proto.CreditApplication.State._
    Seq(ACTIVE, CANCELED)
  }

  override def spec: ZSpec[TestEnvironment, Any] = {
    import CreditApplicationDao._

    (suite("YdbCreditApplicationDao")(
      testM("upsert") {
        val res = transactionally(upsert(creditApplication, shard))
        assertM(res.as(true))(
          isTrue
        )
      },
      testM("get") {
        val res = transactionally(get(GetById(creditApplication.id.taggedWith[Tag.CreditApplicationId])))
        assertM(res.map(_.get.id))(
          equalTo(creditApplication.id)
        )
      },
      testM("listByParams") {
        val res = transactionally(list(ByParams(Domain.DOMAIN_AUTO, user, Seq.empty, 1, 0, withCountTotal = false)))
        assertM(res.map(_.entities.head.id))(
          equalTo(creditApplication.id)
        )
      },
      testM("listByUserWithCount") {
        val res = transactionally(list(ByParams(Domain.DOMAIN_AUTO, user, Seq.empty, 1, 0, withCountTotal = true)))
        res.map(r => assertTrue(r.totalEntities == 1L.some))
      },
      testM("listByUserWithCount & absent states") {
        val res = transactionally(list(ByParams(Domain.DOMAIN_AUTO, user, absentStates, 1, 0, withCountTotal = true)))
        res.map(r => assertTrue(r.totalEntities == 0L.some))
      },
      testM("listByUserWithCount & present states") {
        val res = transactionally(
          list(
            ByParams(Domain.DOMAIN_AUTO, user, Seq(proto.CreditApplication.State.DRAFT), 1, 0, withCountTotal = true)
          )
        )
        res.map(r => assertTrue(r.totalEntities == 1L.some))
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(creditApplicationDaoLayer)
  }
}
