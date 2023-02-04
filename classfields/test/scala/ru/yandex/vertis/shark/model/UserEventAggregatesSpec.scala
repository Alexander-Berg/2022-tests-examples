package ru.yandex.vertis.shark.model

import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.shark.proto.model.UserEvent.UserEventType
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.AutoUser
import ru.yandex.vertis.zio_baker.util.DateTimeUtil.RichInstant
import zio.Task
import zio.test.Assertion.isTrue
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.Instant
import scala.concurrent.duration.DurationInt

object UserEventAggregatesSpec extends DefaultRunnableSpec {

  private val user = AutoUser("test-user-id".taggedWith[zio_baker.Tag.UserId])
  private val domain = Domain.DOMAIN_AUTO

  private val ts = Instant.now()
  private val ts1 = ts.minusDuration(1.second)
  private val ts2 = ts.minusDuration(1.hour)
  private val ts3 = ts.minusDuration(1.day)
  private val ts4 = ts.minusDuration(31.days)

  private val offerId1 = "test-offer-id-1".taggedWith[zio_baker.Tag.OfferId]
  private val offerId2 = "test-offer-id-2".taggedWith[zio_baker.Tag.OfferId]
  private val offerId3 = "test-offer-id-3".taggedWith[zio_baker.Tag.OfferId]

  private val events = Seq(
    UserEvent.CardView(ts4, user, domain, offerId1),
    UserEvent.PhoneCall(ts4, user, domain, offerId1),
    UserEvent.CreditBindingClick(ts4, user, domain),
    UserEvent.CardView(ts3, user, domain, offerId2),
    UserEvent.PhoneCall(ts3, user, domain, offerId2),
    UserEvent.CreditBindingClick(ts3, user, domain),
    UserEvent.CardView(ts2, user, domain, offerId2),
    UserEvent.PhoneCall(ts2, user, domain, offerId2),
    UserEvent.CreditBindingClick(ts2, user, domain),
    UserEvent.CardView(ts1, user, domain, offerId3),
    UserEvent.PhoneCall(ts1, user, domain, offerId3),
    UserEvent.CreditBindingClick(ts1, user, domain)
  )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("UserEventAggregatesSpec")(
      testM("filter events CardView") {
        val res = Task {
          val filtered = UserEventAggregates.filter(UserEventType.CARD_VIEW, events, ts)
          val checkCount = filtered.size == 2
          val checkOffers = {
            val events = filtered.collect { case e: UserEvent.CardView =>
              e
            }
            val e1 = events.exists(o => o.timestamp == ts2 && o.offerId == offerId2)
            val e2 = events.exists(o => o.timestamp == ts1 && o.offerId == offerId3)
            events.size == 2 && e1 && e2
          }
          checkCount && checkOffers
        }
        assertM(res)(isTrue)
      },
      testM("filter events PhoneCall") {
        val res = Task {
          val filtered = UserEventAggregates.filter(UserEventType.PHONE_CALL, events, ts)
          val checkCount = filtered.size == 2
          val checkOffers = {
            val events = filtered.collect { case e: UserEvent.PhoneCall =>
              e
            }
            val e1 = events.exists(o => o.timestamp == ts2 && o.offerId == offerId2)
            val e2 = events.exists(o => o.timestamp == ts1 && o.offerId == offerId3)
            events.size == 2 && e1 && e2
          }
          checkCount && checkOffers
        }
        assertM(res)(isTrue)
      },
      testM("filter events CreditBindingClick") {
        val res = Task {
          val filtered = UserEventAggregates.filter(UserEventType.CREDIT_BINDING_CLICK, events, ts)
          val checkCount = filtered.size == 3
          val checkType = {
            val events = filtered.collect { case e: UserEvent.CreditBindingClick =>
              e
            }
            val e1 = events.exists(_.timestamp == ts1)
            val e2 = events.exists(_.timestamp == ts2)
            val e3 = events.exists(_.timestamp == ts3)
            events.size == 3 && e1 && e2 && e3
          }
          checkCount && checkType
        }
        assertM(res)(isTrue)
      }
    )
}
