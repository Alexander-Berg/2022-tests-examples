package ru.auto.api.routes.v1.notification

import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.wrappers.{StringValue, UInt32Value}
import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.notification.ApiModel.{ListIdsRequest, ListTopicsRequest}
import ru.yandex.vertis.spamalot.{ListResponse, Pagination, ReceivedNotification, UnreadCountResponse}

import scala.jdk.CollectionConverters._

object NotificationModelGenerators extends BasicGenerators {

  val listIdsRequestGen: Gen[ListIdsRequest] = for {
    count <- Gen.posNum[Int]
    code <- Gen.listOfN(count, readableString)
  } yield {
    val builder = ListIdsRequest.newBuilder()
    builder.addAllId(code.asJava)
    builder.build()
  }

  private val testDateTime = new DateTime()

  val paginationGen: Gen[Pagination] = for {
    lastId <- readableString
    limit <- Gen.posNum[Int]
  } yield Pagination
    .newBuilder()
    .setLastId(StringValue.toJavaProto(StringValue(lastId)))
    .setLastTs(Timestamp.toJavaProto(Timestamp(seconds = testDateTime.getMillis / 1000)))
    .setLimit(UInt32Value.toJavaProto(UInt32Value(limit)))
    .build()

  val listTopicsRequestGen: Gen[ListTopicsRequest] = for {
    count <- Gen.posNum[Int]
    code <- Gen.listOfN(count, readableString)
  } yield {
    val builder = ListTopicsRequest.newBuilder()
    builder.addAllTopic(code.asJava)
    builder.build()
  }

  val unreadCountResponseGen: Gen[UnreadCountResponse] = for {
    count <- Gen.posNum[Int]
  } yield UnreadCountResponse
    .newBuilder()
    .setUnreadMessages(count)
    .build()

  val receivedNotificationGen: Gen[ReceivedNotification] = for {
    id <- readableString
  } yield ReceivedNotification
    .newBuilder()
    .setId(id)
    .build()

  val listResponseGen: Gen[ListResponse] = for {
    count <- Gen.posNum[Int]
    notifications <- Gen.listOfN(count, receivedNotificationGen)
  } yield ListResponse.newBuilder().addAllNotifications(notifications.asJava).build

}
