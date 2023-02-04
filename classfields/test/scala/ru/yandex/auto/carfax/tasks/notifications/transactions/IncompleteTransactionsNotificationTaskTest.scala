package ru.yandex.auto.carfax.tasks.notifications.transactions

import cats.syntax.option._
import io.opentracing.noop.NoopTracerFactory
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.carfax.scheduler.tasks.notifications.transactions.IncompleteTransactionsNotificationTask
import ru.yandex.auto.carfax.scheduler.tasks.notifications.transactions.model.OfferTransactionInfo
import ru.yandex.auto.carfax.scheduler.tasks.notifications.transactions.model.standalone.{
  LpStandaloneTransactionInfo,
  VinStandaloneTransactionInfo
}
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.model.{AutoruOfferId, LicensePlate, UserRef, VinCode}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.ReportNotificationState.{NotificationContext, NotificationInfo}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, ReportNotificationState}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.service.licenseplate.LicensePlateUpdateService
import ru.yandex.auto.vin.decoder.service.vin.VinUpdateService
import ru.yandex.auto.vin.decoder.yql.YQLJdbc
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.IterableHasAsJava

class IncompleteTransactionsNotificationTaskTest extends AnyWordSpecLike with MockitoSupport {

  private val yt = mock[Yt]
  private val yql = mock[YQLJdbc]
  private val vinUpdateService = mock[VinUpdateService]
  private val lpUpdateService = mock[LicensePlateUpdateService]
  implicit val tracer = NoopTracerFactory.create()

  val task = new IncompleteTransactionsNotificationTask(
    yt,
    "verticals",
    yql,
    "test_path",
    vinUpdateService,
    lpUpdateService
  )

  "update state func" should {
    "return None" when {
      "all notifications already exists by vin or offer id" in {
        val vin = VinCode("X4XXG55470DS40452")

        val notification1 = buildNotificationInfo("user:1", "123-abc".some, vin.toString.some, none)
        val notification2 = buildNotificationInfo("user:2", none, vin.toString.some, none)

        val transactions = List(
          OfferTransactionInfo(UserRef.user(1), AutoruOfferId.apply(123L, "abc"), vin),
          VinStandaloneTransactionInfo(UserRef.user(2), vin)
        )
        val state = WatchingStateHolder(vin, buildState(List(notification1, notification2)), 100L)

        val res = task.updateStatesFunc[VinCode](transactions)
        transactions.foreach(t => {
          assert(res(t.getCompoundStateIdentifier).apply(state).isEmpty)
        })
      }
      "all notifications already exists by lp" in {
        val lp = LicensePlate("A123AA77")

        val notification1 = buildNotificationInfo("user:1", none, none, lp.toString.some)

        val transaction = LpStandaloneTransactionInfo(UserRef.user(1), lp)
        val state = WatchingStateHolder(lp, buildState(List(notification1)), 100L)

        val res = task.updateStatesFunc[LicensePlate](List(transaction))

        assert(res(transaction.getCompoundStateIdentifier).apply(state).isEmpty)
      }
    }
    "add new notifications" when {
      "notification by offer already exists, but for other user" in {
        val vin = VinCode("X4XXG55470DS40452")

        val notification1 = buildNotificationInfo("user:1", "123-abc".some, vin.toString.some, none)

        val transaction1 = OfferTransactionInfo(UserRef.user(2), AutoruOfferId.apply(123L, "abc"), vin)
        val transaction2 = OfferTransactionInfo(UserRef.user(3), AutoruOfferId.apply(123L, "abc"), vin)
        val state = WatchingStateHolder(vin, buildState(List(notification1)), 100L)

        val res = task.updateStatesFunc[VinCode](List(transaction1, transaction2))

        val updated = res(vin).apply(state)
        assert(updated.nonEmpty)
        assert(updated.get.delay.toDuration == Duration.Zero)
        val updatedState = updated.get.state.getIncompleteTransactionsNotificationState
        assert(updatedState.getInfoCount == 3)
        assert(transaction1.equals(updatedState.getInfo(1)))
        assert(transaction2.equals(updatedState.getInfo(2)))
      }
    }
  }

  private def buildNotificationInfo(
      user: String,
      offerId: Option[String],
      vin: Option[String],
      lp: Option[String]): NotificationInfo = {
    NotificationInfo
      .newBuilder()
      .setCreateTimestamp(123L)
      .setUserId(user)
      .setContext(
        NotificationContext
          .newBuilder()
          .setOfferId(offerId.getOrElse(""))
          .setVin(vin.getOrElse(""))
          .setLicensePlate(lp.getOrElse(""))
      )
      .build()
  }

  private def buildState(notifications: List[NotificationInfo]): CompoundState = {
    val builder = CompoundState.newBuilder()
    builder.setIncompleteTransactionsNotificationState(
      ReportNotificationState
        .newBuilder()
        .addAllInfo(notifications.asJava)
    )
    builder.build()
  }

}
