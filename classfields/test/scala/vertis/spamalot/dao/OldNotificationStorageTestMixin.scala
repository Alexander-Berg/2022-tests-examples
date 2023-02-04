package vertis.spamalot.dao

import com.yandex.ydb.core.UnexpectedResultException
import common.zio.logging.Logging
import ru.yandex.vertis.spamalot.inner.StoredNotification
import ru.yandex.vertis.ydb.zio.TxError.Died
import ru.yandex.vertis.ydb.zio.{Tx, TxEnv, TxRIO, TxTask}
import vertis.core.utils.NoWarnFilters
import vertis.spamalot.dao.queries.notifications.old.AddQuery.{AddBatchQuery, InsertOneQuery, UpsertOneQuery}
import vertis.ydb.exceptions.YdbExceptions.RichUnexpectedResultException
import zio.{UIO, ZIO}

trait OldNotificationStorageTestMixin { self: OldNotificationsStorage =>

  def upsertOne(notification: StoredNotification): TxRIO[Logging.Logging, Unit] = {
    for {
      params <- Tx.fromEffect(
        ZIO
          .fromOption(UpsertOneQuery.makeParams(notification))
          .orElseFail(new IllegalArgumentException(s"Passed notification $notification must be for user"))
      )
      _ <- TxTask(self.validate(notification))
      _ <- prepareAndExecute(
        UpsertOneQuery,
        params
      )
    } yield ()
  }

  def upsert(notifications: Seq[StoredNotification]): TxRIO[Logging.Logging, Unit] = {
    ZIO.when(notifications.nonEmpty) {
      TxTask(notifications.foreach(self.validate)) *>
        prepareAndExecute(
          AddBatchQuery,
          AddBatchQuery.makeParams(notifications)
        ).unit
    }
  }

  def insertOne(notification: StoredNotification): TxRIO[Logging.Logging, Boolean] =
    for {
      params <- Tx.fromEffect(
        ZIO
          .fromOption(InsertOneQuery.makeParams(notification))
          .orElseFail(new IllegalArgumentException(s"Passed notification $notification must be for user"))
      )
      _ <- TxTask(self.validate(notification))
      result <- prepareAndExecute(
        InsertOneQuery,
        params
      ).as(true)
        .catchSome {
          case Died(e: UnexpectedResultException) if e.isPkViolation =>
            TxEnv.accessM[Logging.Logging](lc => Logging.info(s"Got $e").provide(lc)) *>
              UIO(false)
        }
    } yield result

  private def validate(notification: StoredNotification): Unit = {
    val userIdIsPresent = notification.receiverId.flatMap(_.id.userId.map(_.nonEmpty)).getOrElse(false) ||
      notification.userId.nonEmpty: @annotation.nowarn(NoWarnFilters.Deprecation)

    require(userIdIsPresent, "UserId is required")
    require(notification.id.nonEmpty, "Id is required")
  }
}
