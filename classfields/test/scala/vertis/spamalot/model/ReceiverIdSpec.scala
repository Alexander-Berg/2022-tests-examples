package vertis.spamalot.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalactic.TypeCheckedTripleEquals
import org.scalacheck.Gen
import org.scalacheck.magnolia._
import vertis.spamalot.model.ReceiverId.ProtoReceiverIdOps
import ru.yandex.vertis.spamalot.model.{ReceiverId => ProtoReceiverId}
import cats.syntax.option._
import org.scalacheck.Arbitrary

class ReceiverIdSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks with TypeCheckedTripleEquals {

  "toProto" should {
    "convert any ReceiverId to the corresponding proto case" in forAll { receiverId: ReceiverId =>
      val proto = receiverId.proto
      receiverId match {
        case ReceiverId.User(UserId(userId)) => proto.id.userId should ===(userId.some)
        case ReceiverId.DeviceId(deviceId) => proto.id.deviceId should ===(deviceId.some)
      }
    }
  }

  "fromProto" should {
    implicit val protoReceiverIdGen: Gen[ProtoReceiverId] = implicitly[Arbitrary[Option[ReceiverId]]].arbitrary
      .map(_.fold(ProtoReceiverId())(_.proto))

    "convert ProtoReceiverId to the corresponding ReceiverId" in forAll(protoReceiverIdGen) {
      protoReceiverId: ProtoReceiverId =>
        val receiverId = protoReceiverId.toReceiverId
        val expected = protoReceiverId.id match {
          case ProtoReceiverId.Id.UserId(userId) => ReceiverId.User(UserId(userId)).some
          case ProtoReceiverId.Id.DeviceId(deviceId) => ReceiverId.DeviceId(deviceId).some
          case ProtoReceiverId.Id.Empty => None
        }

        receiverId should ===(expected)
    }
  }

}
