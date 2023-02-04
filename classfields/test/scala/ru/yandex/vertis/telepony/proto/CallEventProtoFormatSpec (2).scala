package ru.yandex.vertis.telepony.proto

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.ProtoSpecBase
import ru.yandex.vertis.telepony.model.BeelineCallEventModelGenerators.BeelineCallEventGen
import ru.yandex.vertis.telepony.model.EventModelGenerators._
import ru.yandex.vertis.telepony.model.MttCallEventModelGenerators.MttCallEventGen

/**
  * @author neron
  */
class CallEventProtoFormatSpec extends AnyWordSpec with ScalaCheckPropertyChecks with Matchers with ProtoSpecBase {

  "CallEventProtoFormat" should {
    "transform events" in {
      forAll(EventGen)(test(_, CallEventProtoFormat.EventProtoConversion))
    }

    "transform actions" in {
      forAll(ActionGen)(test(_, CallEventProtoFormat.ActionProtoConversion))
    }

    "transform MtsCallEventAction" in {
      forAll(MtsCallEventActionGen)(test(_, CallEventProtoFormat.MtsCallEventActionProtoConversion))
    }

    "transform VoxCallEventAction" in {
      forAll(VoxCallEventActionGen)(test(_, CallEventProtoFormat.VoxCallEventActionProtoConversion))
    }

    "transform beeline call events" in {
      forAll(BeelineCallEventGen)(test(_, BeelineCallEventProto.BeelineCallEventConversion))
    }

    "transform mtt call events" in {
      forAll(MttCallEventGen)(test(_, MttCallEventProto.MttCallEventConversion))
    }

  }

}
