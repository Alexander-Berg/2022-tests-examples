package vertis.spamalot.dao.queries.notifications.old

import cats.syntax.option._
import com.yandex.ydb.table.query.Params
import monocle.macros.{GenLens, GenPrism}
import monocle.syntax.all._
import org.apache.commons.lang3.StringUtils
import ru.yandex.vertis.spamalot.inner.{StoragePayload, StoredNotification}
import ru.yandex.vertis.spamalot.model.{NotificationObject, ReceiverId => ProtoReceiverId}
import vertis.core.utils.NoWarnFilters
import vertis.spamalot.convert.instances._
import vertis.spamalot.dao.OldNotificationsStorage
import vertis.spamalot.dao.YdbConstants._
import vertis.spamalot.model.UserId
import vertis.ydb.convert.YdbParam
import vertis.ydb.queue.storage.Query

import java.time.Instant

/** Queries for upserting new notifications
 *
 * @author tymur-lysenko
 * @author kusaeva
 */
sealed trait AddQuery extends Query {

  val commonDeclarations: String =
    s"""|${UserIdColumn.declare[UserId]}
        |${IdColumn.declare[String]}
        |${TopicColumn.declare[String]}
        |${CreateTsColumn.declare[Instant]}
        |${NameColumn.declare[Option[String]]}
        |${PayloadColumn.declare[StoragePayload]}
        |${NotifObjectColumn.declare[Option[NotificationObject]]}
        |${IsReadColumn.declare[Boolean]}
        |""".stripMargin

  val commonColumns = Seq(
    UserIdColumn,
    IdColumn,
    TopicColumn,
    CreateTsColumn,
    NameColumn,
    PayloadColumn,
    NotifObjectColumn,
    IsReadColumn
  )

  val commonValues = commonColumns.map(_.param)
}

object AddQuery {

  private def add(ifNotExists: Boolean): String =
    if (ifNotExists) "INSERT"
    else "UPSERT"

  val InsertOneQuery = new AddOneQuery(ifNotExists = true)
  val UpsertOneQuery = new AddOneQuery(ifNotExists = false)
  val InsertCampaignQuery = new AddCampaignQuery(ifNotExists = true)
  val UpsertCampaignQuery = new AddCampaignQuery(ifNotExists = false)

  class AddOneQuery(ifNotExists: Boolean) extends AddQuery {

    override val query: String =
      s"""|$commonDeclarations
          |
          |${add(ifNotExists)} INTO ${OldNotificationsStorage.databaseTableName} (
          |    $PartitionHashColumn,
          |    ${commonColumns.mkString(", ")}
          |) VALUES (
          |    Digest::CityHash($$$UserIdColumn),
          |    ${commonValues.mkString(", ")}
          |);""".stripMargin

    def makeParams(notification: StoredNotification): Option[Params] = {
      import notification._

      for {
        user <- notificationUserId(notification)
      } yield toParams(
        UserIdColumn -> user,
        IdColumn -> id,
        TopicColumn -> topic,
        CreateTsColumn -> createTs,
        NameColumn -> notification.name,
        PayloadColumn -> payload,
        NotifObjectColumn -> notificationObject,
        IsReadColumn -> isRead
      )
    }

    @annotation.nowarn(NoWarnFilters.Deprecation)
    private def notificationUserId(notification: StoredNotification): Option[String] =
      notification
        .focus(_.receiverId)
        .each
        .andThen(GenLens[ProtoReceiverId](_.id))
        .andThen(GenPrism[ProtoReceiverId.Id, ProtoReceiverId.Id.UserId])
        .andThen(GenLens[ProtoReceiverId.Id.UserId](_.value))
        .headOption
        .orElse(
          notification.userId.some.filter(_.nonEmpty)
        )
  }

  class AddCampaignQuery(ifNotExists: Boolean) extends AddQuery {

    override val query: String =
      s"""|$commonDeclarations
          |${CampaignIdColumn.declare[String]}
          |${PayloadIdColumn.declare[String]}
          |
          |${add(ifNotExists)} INTO ${OldNotificationsStorage.databaseTableName} (
          |    $PartitionHashColumn,
          |    ${commonColumns.mkString(",")},
          |    $CampaignIdColumn,
          |    $PayloadIdColumn
          |) VALUES (
          |    Digest::CityHash($$$UserIdColumn),
          |    ${commonValues.mkString(",")},
          |    $$$CampaignIdColumn,
          |    $$$PayloadIdColumn
          |);""".stripMargin
  }

  object AddBatchQuery extends AddQuery {

    private val values = "values"

    implicit private val unknownFields =
      YdbStringParam.inmap[scalapb.UnknownFieldSet](_ => StringUtils.EMPTY, _ => scalapb.UnknownFieldSet())
    implicit private val row = YdbParam.gen[StoredNotification]
    implicit private val rowSeq = seqParam[StoredNotification]

    override def query: String =
      s"""
         |${values.declare[Seq[StoredNotification]]}
         |
         |REPLACE INTO ${OldNotificationsStorage.databaseTableName}
         | ($PartitionHashColumn, ${commonColumns.mkString(", ")})
         |SELECT
         | Digest::CityHash($UserIdColumn), ${commonColumns.mkString(", ")},
         |FROM AS_TABLE($$$values);
         |
         |""".stripMargin

    def makeParams(notifications: Seq[StoredNotification]): Params = {
      val notificationsWithUserId = notifications.zip(notifications.map(_.receiverId)).collect {
        case (n, Some(ProtoReceiverId(ProtoReceiverId.Id.UserId(userId), _))) => n.copy(userId = userId)
        case (n, _) if (n.userId: @annotation.nowarn(NoWarnFilters.Deprecation)).nonEmpty => n
      }

      toParams(values -> notificationsWithUserId)
    }
  }
}
