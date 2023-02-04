package ru.yandex.vertis.shark.dao.impl

import com.softwaremill.tagging.Tagger
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.shark.dao.UserEventAggregatesDao
import ru.yandex.vertis.shark.model.UserEvent.OfferPayload
import ru.yandex.vertis.shark.model.{UserEvent, UserEventAggregates}
import ru.yandex.vertis.shark.proto.model.UserEvent.UserEventType
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.{AutoUser, OfferId, User}
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.clock.Clock
import zio.test.Assertion.isTrue
import zio.test.TestAspect.{beforeAll, sequential}
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.Instant

object YdbUserEventAggregatesDaoImplSpec extends DefaultRunnableSpec {

  private lazy val testLayer =
    Clock.any >+> ydb >+> TransactionSupport.live >+> UserEventAggregatesDao.live

  private val user: User = AutoUser("test-user".taggedWith[zio_baker.Tag.UserId])
  private val domain: Domain = Domain.DOMAIN_AUTO
  private val created: Instant = Instant.now()
  private val updated: Instant = Instant.now()

  private val offerId1: OfferId = "test-offer-id-1".taggedWith[zio_baker.Tag.OfferId]
  private val offerId2: OfferId = "test-offer-id-2".taggedWith[zio_baker.Tag.OfferId]

  private val userEventCardView: UserEvent.CardView =
    UserEvent.CardView(created, user, domain, offerId1)

  private val userEventCreditCalculatorClick: UserEvent.CreditCalculatorClick =
    UserEvent.CreditCalculatorClick(created, user, domain)

  private val userEventPhoneCall: UserEvent.PhoneCall =
    UserEvent.PhoneCall(created, user, domain, offerId2)

  private val events = Seq(userEventCardView, userEventCreditCalculatorClick, userEventPhoneCall)

  private val userEventAggregates: UserEventAggregates =
    UserEventAggregates(user, domain, created, updated, events)

  override def spec: ZSpec[TestEnvironment, Any] = {
    import UserEventAggregatesDao._

    (suite("YdbUserEventAggregatesDao")(
      testM("upsert") {
        val res = for {
          _ <- transactionally(upsert(userEventAggregates))
        } yield true
        assertM(res)(isTrue)
      },
      testM("get") {
        val res =
          for {
            result <- transactionally(get(user))
          } yield result.exists { res =>
            val user = res.user == this.user
            val domain = res.domain == this.domain
            val created = res.created == this.created
            val updated = res.updated == this.updated
            val events = {
              def check(expectedEventType: UserEventType)(event: UserEvent): Boolean = {
                val checkEventType = event.eventType == expectedEventType
                val checkTimestamp = event.timestamp == this.created
                val checkUser = event.user == this.user
                val checkDomain = event.domain == this.domain
                checkEventType && checkTimestamp && checkUser && checkDomain
              }
              def checkWithOfferPayload(
                  expectedEventType: UserEventType,
                  expectedOfferId: OfferId
                )(event: UserEvent with OfferPayload): Boolean =
                check(expectedEventType)(event) && event.offerId == expectedOfferId

              val count = res.events.length == 3
              val cardView = {
                val events = res.events.collect { case e: UserEvent.CardView =>
                  e
                }
                events.length == 1 && events.forall(checkWithOfferPayload(UserEventType.CARD_VIEW, this.offerId1))
              }
              val cardPhoneCall = {
                val events = res.events.collect { case e: UserEvent.PhoneCall =>
                  e
                }
                events.length == 1 && events.forall(checkWithOfferPayload(UserEventType.PHONE_CALL, this.offerId2))
              }
              val cardCreditCalculatorClick = {
                val events = res.events
                  .collect { case e: UserEvent.CreditCalculatorClick =>
                    e
                  }
                events.length == 1 && events.forall(check(UserEventType.CREDIT_CALCULATOR_CLICK))
              }
              count && cardView && cardPhoneCall && cardCreditCalculatorClick
            }
            user && domain && created && updated && events
          }
        assertM(res)(isTrue)
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(testLayer)
  }
}
