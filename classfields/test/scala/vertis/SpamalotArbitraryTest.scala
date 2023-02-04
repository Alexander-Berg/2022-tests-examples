package vertis

import cats.syntax.option._
import com.google.protobuf.timestamp.Timestamp
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.magnolia._
import ru.yandex.vertis.proto.util.convert.ProtoConversions._
import ru.yandex.vertis.spamalot.core.AppVersionLimit
import ru.yandex.vertis.spamalot.inner.OperationPayload.Payload
import ru.yandex.vertis.spamalot.inner.{AddNotification, OperationPayload, SendPush, StoragePayload, StoredNotification}
import ru.yandex.vertis.spamalot.model.Channel.ChannelType
import ru.yandex.vertis.spamalot.model.{Channel, PushIds, ReceiverId => ProtoReceiverId}
import ru.yandex.vertis.util.collection._
import scalapb.UnknownFieldSet
import scalapb.descriptors.{PEmpty, PMessage, PValue}
import vertis.spamalot.model.{ReceiverId, UserId}
import vertis.ydb.unsigned.UnsignedInt

import java.time.Instant
import scala.reflect.runtime.universe._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait SpamalotArbitraryTest extends ArbitraryTestBase {

  implicit lazy val arbitraryTimestamp: Arbitrary[Timestamp] =
    Arbitrary(Instant.now.toProtoTimestamp)

  implicit lazy val arbitraryUnsignedInt: Arbitrary[UnsignedInt] =
    Arbitrary(
      Gen.chooseNum(0L, UnsignedInt.MaxValue).map(UnsignedInt(_))
    )

  implicit val arbitraryUserId: Arbitrary[UserId] =
    Arbitrary(Gen.uuid.map(uuid => UserId(uuid.toString)))

  implicit val arbitraryDeviceId: Arbitrary[ReceiverId.DeviceId] =
    Arbitrary(Gen.uuid.map(uuid => ReceiverId.DeviceId(uuid.toString)))

  implicit val arbitraryUnknownFieldSet: Arbitrary[UnknownFieldSet] =
    Arbitrary(UnknownFieldSet())

  implicit val arbitraryProtoReceiverId: Arbitrary[ProtoReceiverId] = {
    val genProtoReceiverIdId: Gen[ProtoReceiverId.Id] = Gen
      .oneOf(
        Gen.uuid.map(_.toString).map(ProtoReceiverId.Id.UserId),
        Gen.uuid.map(_.toString).map(ProtoReceiverId.Id.DeviceId)
      )
    val result = genProtoReceiverIdId.map(id => ProtoReceiverId(id = id))
    Arbitrary(result)
  }

  implicit val arbitraryOptionProtoReceiverId: Arbitrary[Option[ProtoReceiverId]] =
    Arbitrary(Gen.some(arbitraryProtoReceiverId.arbitrary))

  implicit val arbitraryAppVersionLimitCompare: Arbitrary[AppVersionLimit.AppVersionCompare] =
    Arbitrary(Arbitrary.arbInt.arbitrary.map(AppVersionLimit.AppVersionCompare.fromValue))

  /** Is this generated proto contains an unset oneOf field
    */
  def isEmptyProto(proto: PMessage): Boolean = {
    val hasEmptyOneOfs = proto.value.toVector
      .collect[(Int, PValue)] {
        case (fd, v) if fd.asProto.oneofIndex.isDefined =>
          fd.asProto.oneofIndex.get -> v
      }
      .groupByTuple[Int, PValue, Seq]
      .values
      .exists(_.forall(_ == PEmpty))
    hasEmptyOneOfs || proto.value.valuesIterator.collect { case m: PMessage => m }.exists(isEmptyProto)
  }

  def nonEmptyProto[T <: scalapb.GeneratedMessage: WeakTypeTag: Arbitrary](n: Int): Seq[T] =
    randomThat[T](n)(p => !isEmptyProto(p.toPMessage))

  def randomSends(
      now: Instant,
      n: Int,
      receiverId: ReceiverId = ReceiverId.User(UserId("test_user"))): Seq[OperationPayload] =
    nonEmptyProto[SendPush](n)
      .map { push =>
        push.copy(notification = uniqUnread(push.notification, receiverId))
      }
      .map { push =>
        OperationPayload(
          operationId = push.notification.id,
          receiverId = push.notification.receiverId,
          operationTs = now.toProtoTimestamp,
          payload = Payload.SendPush(push)
        )
      }
      .toIndexedSeq

  private def uniqUnread(notification: StoredNotification, receiverId: ReceiverId) = receiverId match {
    case userReceiverId @ ReceiverId.User(UserId(userId)) =>
      notification.copy(
        id = getUniqString,
        receiverId = userReceiverId.proto.some,
        isRead = false,
        userId = userId,
        payload = fixedPayload(notification.payload)
      )
    case deviceId: ReceiverId.DeviceId =>
      notification.copy(
        id = getUniqString,
        receiverId = deviceId.proto.some,
        isRead = false,
        userId = "",
        payload = fixedPayload(notification.payload)
      )
  }

  /** fix generated groupId so it can be parsed as integer */
  private def fixedPayload(payload: StoragePayload): StoragePayload = {
    val m = payload.payload.media.map { m =>
      if (m.media.isImage) m.withImage(m.getImage.withGroupId("42")) else m
    }
    val p = payload.payload.withMedia(m)
    payload.withPayload(p)
  }

  private val pushChannel = Channel(ChannelType.PUSH)

  def randomOperations(
      now: Instant,
      n: Int,
      receiverId: ReceiverId = ReceiverId.User(UserId("test_user")),
      addPush: Boolean = true): Seq[OperationPayload] =
    nonEmptyProto[AddNotification](n)
      .map { add =>
        val wNotification = add.copy(notification = uniqUnread(add.notification, receiverId))
        if (addPush) {
          wNotification.copy(
            channels = Seq(pushChannel),
            notification = wNotification.notification.copy(
              pushIds = Some(PushIds(random[Int], random[String])),
              createTs = now.toProtoTimestamp
            )
          )
        } else wNotification
      }
      .map { add =>
        OperationPayload(
          operationId = add.notification.id,
          receiverId = add.notification.receiverId,
          operationTs = now.toProtoTimestamp,
          payload = Payload.AddNotification(add)
        )
      }
      .toIndexedSeq
}
