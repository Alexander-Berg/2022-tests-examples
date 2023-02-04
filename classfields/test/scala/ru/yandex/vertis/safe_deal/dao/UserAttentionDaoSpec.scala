package ru.yandex.vertis.safe_deal.dao

import cats.implicits._
import com.softwaremill.tagging.Tagger
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.safe_deal.model.{Attention, AutoruSubject, Tag}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{beforeAll, sequential}
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object UserAttentionDaoSpec extends DefaultRunnableSpec {

  private lazy val userAttentionDaoLayer =
    Clock.any >+> ydb >+> TransactionSupport.live >+> UserAttentionDao.live

  private val userId = "user_id".taggedWith[zio_baker.Tag.UserId]
  private val dealId1 = "deal_id:1".taggedWith[Tag.DealId]
  private val dealId2 = "deal_id:2".taggedWith[Tag.DealId]
  private lazy val created = Instant.now
  private lazy val updated1 = created
  private lazy val updated2 = created.plusSeconds(30)
  private val isRead = false
  private val template = "template".taggedWith[Tag.NotificationTemplateId]
  private val params = Map("car" -> "car", "dealId" -> "dealId")
  private val subject = AutoruSubject(None, None, None, None, None, None, None).some

  private lazy val attention1 = Attention(userId, dealId1, created, updated1, isRead, template, Map.empty, subject)
  private lazy val attention2 = Attention(userId, dealId2, created, updated1, isRead, template, params, subject)

  implicit private class RichAttention(val value: Attention) extends AnyVal {

    def withFixedTimestams: Attention = value.withFixedCreated.withFixedUpdated(updated1)

    def withFixedCreated: Attention = value.copy(created = created)

    def withFixedUpdated(ts: Instant): Attention = value.copy(updated = ts)
  }

  override def spec: ZSpec[TestEnvironment, Any] = {
    import ru.yandex.vertis.safe_deal.dao.UserAttentionDao._

    (suite("UserAttentionDao")(
      testM("upsert") {
        val res1 = transactionally(upsert(attention1)).as(true)
        val res2 = transactionally(upsert(attention2)).as(true)
        val res = res1.zipWith(res2)(_ && _)
        assertM(res)(isTrue)
      },
      testM("unread by user_id") {
        val res = transactionally(last(userId, None, unreadOnly = true))
          .map(_.map(_.withFixedTimestams))
        assertM(res)(equalTo(Seq(attention1, attention2)))
      },
      testM("mark as read") {
        val res = transactionally(read(userId, dealId1.some, updated2)).as(true)
        assertM(res)(isTrue)
      },
      testM("unread by user_id & deal_id") {
        val impl =
          for {
            res1 <- transactionally(last(userId, dealId1.some, unreadOnly = true))
              .map(_.map(_.withFixedTimestams))
            res2 <- transactionally(last(userId, dealId2.some, unreadOnly = true))
              .map(_.map(_.withFixedTimestams))
          } yield assertTrue(
            res1 == Seq.empty,
            res2 == Seq(attention2)
          )
        impl
      },
      testM("last") {
        val res = transactionally(last(userId, dealId1.some, unreadOnly = false))
          .map(_.map(_.withFixedCreated.withFixedUpdated(updated2)))
        assertM(res)(equalTo(Seq(attention1.copy(updated = updated2, read = true))))
      },
      testM("delete") {
        val res = transactionally(delete(userId, dealId1)) *>
          transactionally(last(userId, dealId1.some, unreadOnly = false))
        assertM(res)(equalTo(Seq.empty))
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(userAttentionDaoLayer)
  }
}
